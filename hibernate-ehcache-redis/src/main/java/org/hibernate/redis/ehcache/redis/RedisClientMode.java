package org.hibernate.redis.ehcache.redis;

public enum RedisClientMode {
	LOCAL,
	SENTINEL,
	SINGLE_SERVER;
	
	public static final String CACHE_MODE_REDIS = "cache-mode-redis";
	public static final String REDIS_SENTINEL = "redis-sentinel";
	
	public boolean isRedisEnabled() {
	      return this != LOCAL;
	   }
}
