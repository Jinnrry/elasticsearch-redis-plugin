package com.jinnrry;


import org.apache.lucene.index.LeafReaderContext;
import org.elasticsearch.script.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.Set;

public class RedisScriptEngine implements ScriptEngine {
    private static JedisPool pool;

    public static void setPool(JedisPool pool) {
        RedisScriptEngine.pool = pool;
    }


    @Override
    public String getType() {
        return "redis";
    }

    @Override
    public <FactoryType> FactoryType compile(String name, String code, ScriptContext<FactoryType> context, Map<String, String> params) {

        if (!context.equals(FilterScript.CONTEXT) && !context.equals(ScoreScript.CONTEXT)) {
            throw new IllegalArgumentException(getType() + " scripts cannot be used for context [" + context.name + "]");
        }

        if ("filter".equals(code)) {
            FilterScript.Factory factory = (p, lookup) -> (FilterScript.LeafFactory) ctx ->
                    new FilterScript(p, lookup, ctx) {
                        @Override
                        public boolean execute() {
                            if (!p.containsKey("key_pre")) {
                                throw new IllegalArgumentException("[key_pre] must be include params!");
                            }
                            if (!p.containsKey("field")) {
                                throw new IllegalArgumentException("[field] must be include params!");
                            }
                            String key_pre = p.get("key_pre").toString();
                            String key_flied = p.get("field").toString();

                            // redis key
                            String cacheKey = key_pre + lookup.source().get(key_flied).toString();
//                            System.out.println(cacheKey);

                            // get data from redis
                            double redis_data = getDataByRedis(cacheKey);
//                            System.out.println("redis data:" + redis_data);

                            // get filter params
                            if (p.containsKey("lt")) {
                                double lt = Double.parseDouble(p.get("lt").toString());
                                if (redis_data >= lt) return false;
                            }
                            if (p.containsKey("gt")) {
                                double gt = Double.parseDouble(p.get("gt").toString());
                                if (redis_data <= gt) return false;
                            }
                            if (p.containsKey("eq")) {
                                double eq = Double.parseDouble(p.get("eq").toString());
                                if (redis_data != eq) return false;
                            }
                            if (p.containsKey("lte")) {
                                double lte = Double.parseDouble(p.get("lte").toString());
                                if (redis_data > lte) return false;
                            }
                            if (p.containsKey("gte")) {
                                double gte = Double.parseDouble(p.get("gte").toString());
                                if (redis_data < gte) return false;
                            }


                            return true;
                        }
                    };
            return context.factoryClazz.cast(factory);
        }

        if ("score".equals(code)) {
            ScoreScript.Factory factory = ((p, searchLookup) -> new ScoreScript.LeafFactory() {

                @Override
                public boolean needs_score() {
                    return false;
                }

                @Override
                public ScoreScript newInstance(DocReader docReader) {
                    return new ScoreScript(p, searchLookup, docReader) {
                        @Override
                        public double execute(ExplanationHolder explanationHolder) {
                            if (!p.containsKey("key_pre")) {
                                throw new IllegalArgumentException("[key_pre] must be include params!");
                            }
                            if (!p.containsKey("field")) {
                                throw new IllegalArgumentException("[field] must be include params!");
                            }

                            String key_pre = p.get("key_pre").toString();
                            String key_flied = p.get("field").toString();

                            // redis key
                            String cacheKey = key_pre + searchLookup.source().get(key_flied).toString();

                            // get data from redis
                            return getDataByRedis(cacheKey);
                        }

                    };
                }
            });

            return context.factoryClazz.cast(factory);
        }

        throw new IllegalArgumentException("Unknown script name " + code);
    }

    @Override
    public void close() throws IOException {
        ScriptEngine.super.close();
    }

    @Override
    public Set<ScriptContext<?>> getSupportedContexts() {
        return Set.of(FilterScript.CONTEXT);
    }


    private static double getDataByRedis(final String key) {
        assert key != null;

        return AccessController.doPrivileged((PrivilegedAction<Double>) () -> {
            try (Jedis jedis = pool.getResource()) {
                String redis_data = jedis.get(key);
                return Double.parseDouble(redis_data);
            } catch (Exception e) {
                return 0.0;
            }
        });
    }

}