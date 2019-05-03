package org.hibernate.redis.ehcache.redis;

public enum RedisClientMode {
	LOCAL,
	SENTINEL,
	SINGLE_SERVER;
	
	public static final String REDIS_SINGLE = "single";
	public static final String REDIS_SENTINEL = "sentinel";
	
	public boolean isRedisEnabled() {
	      return this != LOCAL;
	   }
}
