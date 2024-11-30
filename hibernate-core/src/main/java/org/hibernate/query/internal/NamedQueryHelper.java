/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import java.util.Map;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.jpa.internal.util.FlushModeTypeHelper;
import org.hibernate.jpa.internal.util.LockModeTypeHelper;

import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;

import static org.hibernate.jpa.HibernateHints.HINT_CACHEABLE;
import static org.hibernate.jpa.HibernateHints.HINT_CACHE_MODE;
import static org.hibernate.jpa.HibernateHints.HINT_CACHE_REGION;
import static org.hibernate.jpa.HibernateHints.HINT_FLUSH_MODE;
import static org.hibernate.jpa.HibernateHints.HINT_NATIVE_LOCK_MODE;
import static org.hibernate.jpa.HibernateHints.HINT_READ_ONLY;

/**
 * @author Steve Ebersole
 */
public class NamedQueryHelper {
	public static abstract class NamedQueryDescriptorBuilder {
		private final String name;
		private final Map<String, Object> hints;

		private Boolean cacheable;
		private String cacheRegion;
		private CacheMode cacheMode;

		private FlushMode flushMode;
		private Boolean readOnly;

		private LockOptions lockOptions;

		private Integer timeout;
		private Integer fetchSize;

		private String comment;

		public NamedQueryDescriptorBuilder(
				String name,
				Map<String, Object> hints,
				SessionFactoryImplementor sessionFactory) {
			this.name = name;
			this.hints = hints;

			cacheable = isCacheable( hints, sessionFactory );
			cacheRegion = cacheable ? determineCacheRegion( hints, sessionFactory ) : null;
			cacheMode = cacheable ? determineCacheMode( hints, sessionFactory ) : null;

			flushMode = determineFlushMode( hints, sessionFactory );
			readOnly = ConfigurationHelper.getBoolean( HINT_READ_ONLY, hints );

			lockOptions = determineLockOptions( hints, sessionFactory );

			timeout = determineTimeout( hints, sessionFactory );

		}

		public String getName() {
			return name;
		}

		public Map<String, Object> getHints() {
			return hints;
		}

		public Boolean getCacheable() {
			return cacheable;
		}

		public void setCacheable(Boolean cacheable) {
			this.cacheable = cacheable;
		}

		public String getCacheRegion() {
			return cacheRegion;
		}

		public void setCacheRegion(String cacheRegion) {
			this.cacheRegion = cacheRegion;
		}

		public CacheMode getCacheMode() {
			return cacheMode;
		}

		public void setCacheMode(CacheMode cacheMode) {
			this.cacheMode = cacheMode;
		}

		public FlushMode getFlushMode() {
			return flushMode;
		}

		public void setFlushMode(FlushMode flushMode) {
			this.flushMode = flushMode;
		}

		public Boolean getReadOnly() {
			return readOnly;
		}

		public void setReadOnly(Boolean readOnly) {
			this.readOnly = readOnly;
		}

		public LockOptions getLockOptions() {
			return lockOptions;
		}

		public void setLockOptions(LockOptions lockOptions) {
			this.lockOptions = lockOptions;
		}

		public Integer getTimeout() {
			return timeout;
		}

		public void setTimeout(Integer timeout) {
			this.timeout = timeout;
		}

		public Integer getFetchSize() {
			return fetchSize;
		}

		public void setFetchSize(Integer fetchSize) {
			this.fetchSize = fetchSize;
		}

		public String getComment() {
			return comment;
		}

		public void setComment(String comment) {
			this.comment = comment;
		}
	}

	private static boolean isCacheable(Map<String, Object> hints, SessionFactoryImplementor sessionFactory) {
		return sessionFactory.getSessionFactoryOptions().isQueryCacheEnabled()
				&& ConfigurationHelper.getBoolean( HINT_CACHEABLE, hints );
	}

	private static String determineCacheRegion(Map<String, Object> hints, SessionFactoryImplementor sessionFactory) {
		assert sessionFactory.getSessionFactoryOptions().isQueryCacheEnabled();
		return ConfigurationHelper.getString( HINT_CACHE_REGION, hints );
	}

	private static CacheMode determineCacheMode(Map<String, Object> hints, SessionFactoryImplementor sessionFactory) {
		assert sessionFactory.getSessionFactoryOptions().isQueryCacheEnabled();
		final Object setting = hints.get( HINT_CACHE_MODE );

		if ( setting != null ) {
			if ( setting instanceof CacheMode ) {
				return (CacheMode) setting;
			}

			final CacheMode cacheMode = CacheMode.interpretExternalSetting( setting.toString() );
			if ( cacheMode != null ) {
				return cacheMode;
			}
		}

		return CacheMode.NORMAL;
	}

	private static FlushMode determineFlushMode(Map<String, Object> hints, SessionFactoryImplementor sessionFactory) {
		final Object setting = hints.get( HINT_FLUSH_MODE );

		if ( setting != null ) {
			if ( setting instanceof FlushMode ) {
				return (FlushMode) setting;
			}

			if ( setting instanceof FlushModeType ) {
				return FlushModeTypeHelper.getFlushMode( (FlushModeType) setting );
			}

			final FlushMode mode = FlushMode.interpretExternalSetting( setting.toString() );
			if ( mode != null ) {
				return mode;
			}
		}

		return FlushMode.AUTO;
	}

	private static LockOptions determineLockOptions(Map<String, Object> hints, SessionFactoryImplementor sessionFactory) {
		final Object lockModeSetting = hints.get( HINT_NATIVE_LOCK_MODE );
		final LockMode lockMode;
		if ( lockModeSetting == null ) {
			lockMode = LockMode.NONE;
		}
		else if ( lockModeSetting instanceof LockMode ) {
			lockMode = (LockMode) lockModeSetting;
		}
		else if ( lockModeSetting instanceof LockModeType ) {
			lockMode = LockModeTypeHelper.getLockMode( (LockModeType) lockModeSetting );
		}
		else {
			lockMode = LockMode.fromExternalForm( lockModeSetting.toString() );
		}

		if ( lockMode == LockMode.NONE ) {
			return LockOptions.NONE;
		}
		else {
			return new LockOptions( lockMode );
		}
	}

	private static Integer determineTimeout(Map<String, Object> hints, SessionFactoryImplementor sessionFactory) {
		return null;
	}
}
