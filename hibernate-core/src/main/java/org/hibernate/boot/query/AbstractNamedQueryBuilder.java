/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.query;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.models.spi.AnnotationTarget;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractNamedQueryBuilder<R, T extends AbstractNamedQueryBuilder<R, T>> {
	private final String name;
	private @Nullable Class<R> resultClass;

	private Boolean cacheable;
	private String cacheRegion;
	private CacheMode cacheMode;

	private FlushMode flushMode;
	private Boolean readOnly;

	private LockOptions lockOptions;

	private Integer timeout;
	private Integer fetchSize;

	private String comment;

	private Map<String, Object> hints;

	private final AnnotationTarget location;

	public AbstractNamedQueryBuilder(String name, AnnotationTarget location) {
		this.name = name;
		this.location = location;
	}

	public String getName() {
		return name;
	}

	AnnotationTarget getLocation() {
		return location;
	}

	protected abstract T getThis();

	public T setResultClass(Class<R> resultClass) {
		if ( resultClass != void.class ) {
			this.resultClass = resultClass;
		}
		return getThis();
	}

	public T setCacheable(Boolean cacheable) {
		this.cacheable = cacheable;
		return getThis();
	}

	public T setCacheRegion(String cacheRegion) {
		this.cacheRegion = cacheRegion;
		return getThis();
	}

	public T setCacheMode(CacheMode cacheMode) {
		this.cacheMode = cacheMode;
		return getThis();
	}

	public T setLockOptions(LockOptions lockOptions) {
		this.lockOptions = lockOptions;
		return getThis();
	}

	public T setTimeout(Integer timeout) {
		this.timeout = timeout;
		return getThis();
	}

	public T setFlushMode(FlushMode flushMode) {
		this.flushMode = flushMode;
		return getThis();
	}

	public T setReadOnly(Boolean readOnly) {
		this.readOnly = readOnly;
		return getThis();
	}

	public T setFetchSize(Integer fetchSize) {
		this.fetchSize = fetchSize;
		return getThis();
	}

	public T setComment(String comment) {
		this.comment = comment;
		return getThis();
	}

	public Class<R> getResultClass() {
		return resultClass;
	}

	public Boolean getCacheable() {
		return cacheable;
	}

	public String getCacheRegion() {
		return cacheRegion;
	}

	public CacheMode getCacheMode() {
		return cacheMode;
	}

	public FlushMode getFlushMode() {
		return flushMode;
	}

	public Boolean getReadOnly() {
		return readOnly;
	}

	public LockOptions getLockOptions() {
		return lockOptions;
	}

	public Integer getTimeout() {
		return timeout;
	}

	public Integer getFetchSize() {
		return fetchSize;
	}

	public String getComment() {
		return comment;
	}

	public void addHint(String name, Object value) {
		if ( hints == null ) {
			hints = new HashMap<>();
		}
		hints.put( name, value );
	}

	public T addHints(Map<String, Object> hintsMap) {
		if ( hints == null ) {
			hints = new HashMap<>();
		}
		hints.putAll( hintsMap );

		return getThis();
	}

	public Map<String, Object> getHints() {
		return hints;
	}
}
