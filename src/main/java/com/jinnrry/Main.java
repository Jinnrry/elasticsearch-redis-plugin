package com.jinnrry;


import org.elasticsearch.SpecialPermission;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.ScriptPlugin;
import org.elasticsearch.plugins.SearchPlugin;
import org.elasticsearch.script.ScriptContext;
import org.elasticsearch.script.ScriptEngine;
import redis.clients.jedis.JedisPool;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;


public class Main extends Plugin implements ScriptPlugin, SearchPlugin {

    private static final String RedisConfigKey = "esHotelPlugin.redis.url";

    public Main(final Settings settings, final Path configPath) {
        JedisPool jedisPool = connectRedis(settings.get(RedisConfigKey));

        RedisScriptEngine.setPool(jedisPool);
    }

    /**
     * @return the plugin's custom settings
     */
    @Override
    public List<Setting<?>> getSettings() {
        List<Setting<?>> settings = new ArrayList<>();

        settings.add(new Setting<>(RedisConfigKey, "redis://127.0.0.1:6379", Function.identity(),
                Setting.Property.NodeScope));

        return settings;
    }


    @Override
    public ScriptEngine getScriptEngine(Settings settings, Collection<ScriptContext<?>> contexts) {

        return new RedisScriptEngine();
    }


    private static JedisPool connectRedis(String redisUrl) {
        SpecialPermission.check();
        return java.security.AccessController.doPrivileged((java.security.PrivilegedAction<JedisPool>) () -> new JedisPool(redisUrl));
    }

}
