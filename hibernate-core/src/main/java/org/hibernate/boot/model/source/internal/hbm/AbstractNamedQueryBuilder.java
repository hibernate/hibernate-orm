/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.Timeout;
import org.hibernate.CacheMode;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.query.QueryFlushMode;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractNamedQueryBuilder<R, T extends AbstractNamedQueryBuilder<R, T>> {
	protected final String name;
	protected final AnnotationTarget location;

	protected @Nullable Class<R> resultClass;

	protected Boolean cacheable;
	protected String cacheRegion;
	protected CacheMode cacheMode;

	protected QueryFlushMode flushMode;
	protected Boolean readOnly;

	protected Timeout timeout;
	protected Integer fetchSize;

	protected String comment;

	protected Map<String, Object> hints;

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

	public T setTimeout(Timeout timeout) {
		this.timeout = timeout;
		return getThis();
	}

	public T setFlushMode(QueryFlushMode flushMode) {
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

	public QueryFlushMode getFlushMode() {
		return flushMode;
	}

	public Boolean getReadOnly() {
		return readOnly;
	}

	public Timeout getTimeout() {
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
