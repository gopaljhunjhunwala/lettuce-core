/*
 * Copyright 2011-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.models.command;

import java.util.*;

import io.lettuce.core.internal.LettuceAssert;

/**
 * Parser for Redis
 * <a href="http://redis.io/commands/command">COMMAND</a>/<a href="http://redis.io/commands/command-info">COMMAND INFO</a>
 * output.
 *
 * @author Mark Paluch
 * @author Mikhael Sokolov
 * @since 3.0
 */
@SuppressWarnings("serial")
public class CommandDetailParser {

    /**
     * Number of array elements for a specific command.
     */
    public static final int COMMAND_INFO_SIZE = 6;

    @SuppressWarnings("serial")
    protected static final Map<String, CommandDetail.Flag> FLAG_MAPPING;

    @SuppressWarnings("serial")
    protected static final Map<String, CommandDetail.AclCategory> ACL_CATEGORY_MAPPING;

    static {
        Map<String, CommandDetail.Flag> flagMap = new HashMap<>();
        flagMap.put("admin", CommandDetail.Flag.ADMIN);
        flagMap.put("asking", CommandDetail.Flag.ASKING);
        flagMap.put("denyoom", CommandDetail.Flag.DENYOOM);
        flagMap.put("fast", CommandDetail.Flag.FAST);
        flagMap.put("loading", CommandDetail.Flag.LOADING);
        flagMap.put("noscript", CommandDetail.Flag.NOSCRIPT);
        flagMap.put("movablekeys", CommandDetail.Flag.MOVABLEKEYS);
        flagMap.put("pubsub", CommandDetail.Flag.PUBSUB);
        flagMap.put("random", CommandDetail.Flag.RANDOM);
        flagMap.put("readonly", CommandDetail.Flag.READONLY);
        flagMap.put("skip_monitor", CommandDetail.Flag.SKIP_MONITOR);
        flagMap.put("sort_for_script", CommandDetail.Flag.SORT_FOR_SCRIPT);
        flagMap.put("stale", CommandDetail.Flag.STALE);
        flagMap.put("write", CommandDetail.Flag.WRITE);
        FLAG_MAPPING = Collections.unmodifiableMap(flagMap);

        Map<String, CommandDetail.AclCategory> aclCategoriesMap = new HashMap<>();
        aclCategoriesMap.put("@keyspace", CommandDetail.AclCategory.KEYSPACE);
        aclCategoriesMap.put("@read", CommandDetail.AclCategory.READ);
        aclCategoriesMap.put("@write", CommandDetail.AclCategory.WRITE);
        aclCategoriesMap.put("@set", CommandDetail.AclCategory.SET);
        aclCategoriesMap.put("@sortedset", CommandDetail.AclCategory.SORTEDSET);
        aclCategoriesMap.put("@list", CommandDetail.AclCategory.LIST);
        aclCategoriesMap.put("@hash", CommandDetail.AclCategory.HASH);
        aclCategoriesMap.put("@string", CommandDetail.AclCategory.STRING);
        aclCategoriesMap.put("@bitmap", CommandDetail.AclCategory.BITMAP);
        aclCategoriesMap.put("@hyperloglog", CommandDetail.AclCategory.HYPERLOGLOG);
        aclCategoriesMap.put("@geo", CommandDetail.AclCategory.GEO);
        aclCategoriesMap.put("@stream", CommandDetail.AclCategory.STREAM);
        aclCategoriesMap.put("@pubsub", CommandDetail.AclCategory.PUBSUB);
        aclCategoriesMap.put("@admin", CommandDetail.AclCategory.ADMIN);
        aclCategoriesMap.put("@fast", CommandDetail.AclCategory.FAST);
        aclCategoriesMap.put("@slow", CommandDetail.AclCategory.SLOW);
        aclCategoriesMap.put("@blocking", CommandDetail.AclCategory.BLOCKING);
        aclCategoriesMap.put("@dangerous", CommandDetail.AclCategory.DANGEROUS);
        aclCategoriesMap.put("@connection", CommandDetail.AclCategory.CONNECTION);
        aclCategoriesMap.put("@transaction", CommandDetail.AclCategory.TRANSACTION);
        aclCategoriesMap.put("@scripting", CommandDetail.AclCategory.SCRIPTING);
        ACL_CATEGORY_MAPPING = Collections.unmodifiableMap(aclCategoriesMap);
    }

    private CommandDetailParser() {
    }

    /**
     * Parse the output of the Redis COMMAND/COMMAND INFO command and convert to a list of {@link CommandDetail}.
     *
     * @param commandOutput the command output, must not be {@code null}
     * @return RedisInstance
     */
    public static List<CommandDetail> parse(List<?> commandOutput) {
        LettuceAssert.notNull(commandOutput, "CommandOutput must not be null");

        List<CommandDetail> result = new ArrayList<>();

        for (Object o : commandOutput) {
            if (!(o instanceof Collection<?>)) {
                continue;
            }

            Collection<?> collection = (Collection<?>) o;
            if (collection.size() < COMMAND_INFO_SIZE) {
                continue;
            }

            CommandDetail commandDetail = parseCommandDetail(collection);
            result.add(commandDetail);
        }

        return Collections.unmodifiableList(result);
    }

    private static CommandDetail parseCommandDetail(Collection<?> collection) {
        Iterator<?> iterator = collection.iterator();
        String name = (String) iterator.next();
        int arity = Math.toIntExact(getLongFromIterator(iterator, 0));
        Object flags = iterator.next();
        int firstKey = Math.toIntExact(getLongFromIterator(iterator, 0));
        int lastKey = Math.toIntExact(getLongFromIterator(iterator, 0));
        int keyStepCount = Math.toIntExact(getLongFromIterator(iterator, 0));
        Object categories = iterator.hasNext() ? iterator.next() : null;

        Set<CommandDetail.Flag> parsedFlags = parseFlags(flags);
        Set<CommandDetail.AclCategory> parsedAclCategories = parseAclCategories(categories);

        return new CommandDetail(name, arity, parsedFlags, firstKey, lastKey, keyStepCount, parsedAclCategories);
    }

    private static Set<CommandDetail.Flag> parseFlags(Object flags) {
        Set<CommandDetail.Flag> result = new HashSet<>();

        if (flags instanceof Collection<?>) {
            Collection<?> collection = (Collection<?>) flags;
            for (Object o : collection) {
                CommandDetail.Flag flag = FLAG_MAPPING.get(o);
                if (flag != null) {
                    result.add(flag);
                }
            }
        }

        return Collections.unmodifiableSet(result);
    }

    private static Set<CommandDetail.AclCategory> parseAclCategories(Object aclCategories) {
        Set<CommandDetail.AclCategory> result = new HashSet<>();

        if (aclCategories instanceof Collection<?>) {
            Collection<?> collection = (Collection<?>) aclCategories;
            for (Object o : collection) {
                CommandDetail.AclCategory aclCategory = ACL_CATEGORY_MAPPING.get(o);
                if (aclCategory != null) {
                    result.add(aclCategory);
                }
            }
        }

        return Collections.unmodifiableSet(result);
    }

    private static long getLongFromIterator(Iterator<?> iterator, long defaultValue) {
        if (iterator.hasNext()) {
            Object object = iterator.next();
            if (object instanceof String) {
                return Long.parseLong((String) object);
            }

            if (object instanceof Number) {
                return ((Number) object).longValue();
            }
        }
        return defaultValue;
    }

}
