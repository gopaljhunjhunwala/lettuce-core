// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.output;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.internal.LettuceLists;
import com.lambdaworks.redis.internal.LettuceMaps;

/**
 * {@link java.util.List} of maps output.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * 
 * @author Will Glozer
 */
public class ListOfMapsOutput<K, V> extends CommandOutput<K, V, List<Map<K, V>>> {
    private MapOutput<K, V> nested;
    private int mapCount = -1;
    private List<Integer> counts = LettuceLists.newList();

    public ListOfMapsOutput(RedisCodec<K, V> codec) {
        super(codec, LettuceLists.newList());
        nested = new MapOutput<K, V>(codec);
    }

    @Override
    public void set(ByteBuffer bytes) {
        nested.set(bytes);
    }

    @Override
    public void complete(int depth) {

        if (!counts.isEmpty()) {
            int expectedSize = counts.get(0);

            if (nested.get().size() == expectedSize) {
                counts.remove(0);
                output.add(LettuceMaps.newLinkedHashMap(nested.get()));
                nested.get().clear();
            }
        }
    }

    @Override
    public void multi(int count) {
        if (mapCount == -1) {
            mapCount = count;
        } else {
            // div 2 because of key value pair counts twice
            counts.add(count / 2);
        }
    }
}
