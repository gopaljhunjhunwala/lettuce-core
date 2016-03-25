package com.lambdaworks.redis.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

import java.net.ConnectException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.lambdaworks.TestClientResources;
import com.lambdaworks.redis.*;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.internal.LettuceLists;
import com.lambdaworks.redis.internal.LettuceSets;
import com.lambdaworks.redis.resource.ClientResources;

/**
 * @author Mark Paluch
 */
public class ClusterPartiallyDownTest extends AbstractTest {
    private static ClientResources clientResources = TestClientResources.create();

    private static int port1 = 7579;
    private static int port2 = 7580;
    private static int port3 = 7581;
    private static int port4 = 7582;

    private static final RedisURI URI_1 = RedisURI.create(TestSettings.host(), port1);
    private static final RedisURI URI_2 = RedisURI.create(TestSettings.host(), port2);
    private static final RedisURI URI_3 = RedisURI.create(TestSettings.host(), port3);
    private static final RedisURI URI_4 = RedisURI.create(TestSettings.host(), port4);

    private RedisClusterClient redisClusterClient;

    @Before
    public void before() throws Exception {

    }

    @After
    public void after() throws Exception {
        redisClusterClient.shutdown();
    }

    @Test
    public void connectToPartiallyDownCluster() throws Exception {

        List<RedisURI> seed = ImmutableList.of(URI_1, URI_2, URI_3, URI_4);
        redisClusterClient = RedisClusterClient.create(clientResources, seed);
        StatefulRedisClusterConnection<String, String> connection = redisClusterClient.connect();

        assertThat(connection.sync().ping()).isEqualTo("PONG");

        connection.close();
    }

    @Test
    public void operateOnPartiallyDownCluster() throws Exception {

        List<RedisURI> seed = ImmutableList.of(URI_1, URI_2, URI_3, URI_4);
        redisClusterClient = RedisClusterClient.create(clientResources, seed);
        StatefulRedisClusterConnection<String, String> connection = redisClusterClient.connect();

        String key_10439 = "aaa";
        assertThat(SlotHash.getSlot(key_10439)).isEqualTo(10439);

        try {
            connection.sync().get(key_10439);
            fail("Missing RedisException");
        } catch (RedisException e) {
            assertThat(e).hasCauseExactlyInstanceOf(RedisConnectionException.class).hasRootCauseInstanceOf(
                    ConnectException.class);
        }

        connection.close();
    }

    @Test
    public void seedNodesAreOffline() throws Exception {

        List<RedisURI> seed = ImmutableList.of(URI_1, URI_2, URI_3);
        redisClusterClient = RedisClusterClient.create(clientResources, seed);

        try {
            redisClusterClient.connect();
            fail("Missing RedisException");
        } catch (RedisException e) {
            assertThat(e).hasNoCause();
        }
    }

    @Test
    public void partitionNodesAreOffline() throws Exception {

        List<RedisURI> seed = ImmutableList.of(URI_1, URI_2, URI_3);
        redisClusterClient = RedisClusterClient.create(clientResources, seed);

        Partitions partitions = new Partitions();
        partitions.addPartition(
                new RedisClusterNode(URI_1, "a", true, null, 0, 0, 0, LettuceLists.newList(), LettuceSets.newHashSet()));
        partitions.addPartition(
                new RedisClusterNode(URI_2, "b", true, null, 0, 0, 0, LettuceLists.newList(), LettuceSets.newHashSet()));

        redisClusterClient.setPartitions(partitions);

        try {
            redisClusterClient.connect();
            fail("Missing RedisConnectionException");
        } catch (RedisConnectionException e) {
            assertThat(e).hasRootCauseInstanceOf(ConnectException.class);
        }
    }
}
