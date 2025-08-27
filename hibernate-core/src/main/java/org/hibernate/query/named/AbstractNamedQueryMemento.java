/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.named;

import java.util.Map;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @author Steve Ebersole
 * @author Gavin King
 */
public abstract class AbstractNamedQueryMemento<R> implements NamedQueryMemento<R> {
	private final String name;
	private final @Nullable Class<R> resultType;

	private final Boolean cacheable;
	private final String cacheRegion;
	private final CacheMode cacheMode;

	private final FlushMode flushMode;
	private final Boolean readOnly;

	private final Integer timeout;
	private final Integer fetchSize;

	private final String comment;

	private final Map<String, Object> hints;

	protected AbstractNamedQueryMemento(
			String name,
			@Nullable Class<R> resultType,
			Boolean cacheable,
			String cacheRegion,
			CacheMode cacheMode,
			FlushMode flushMode,
			Boolean readOnly,
			Integer timeout,
			Integer fetchSize,
			String comment,
			Map<String, Object> hints) {
		this.name = name;
		this.resultType = resultType;
		this.cacheable = cacheable;
		this.cacheRegion = cacheRegion;
		this.cacheMode = cacheMode;
		this.flushMode = flushMode;
		this.readOnly = readOnly;
		this.timeout = timeout;
		this.fetchSize = fetchSize;
		this.comment = comment;
		this.hints = hints;
	}

	@Override
	public String getRegistrationName() {
		return name;
	}

	@Override
	public @Nullable Class<R> getResultType() {
		return resultType;
	}

	@Override
	public Boolean getCacheable() {
		return cacheable;
	}

	@Override
	public String getCacheRegion() {
		return cacheRegion;
	}

	@Override
	public CacheMode getCacheMode() {
		return cacheMode;
	}

	@Override
	public FlushMode getFlushMode() {
		return flushMode;
	}

	@Override
	public Boolean getReadOnly() {
		return readOnly;
	}

	@Override
	public Integer getTimeout() {
		return timeout;
	}

	@Override
	public Integer getFetchSize() {
		return fetchSize;
	}

	@Override
	public String getComment() {
		return comment;
	}

	@Override
	public Map<String, Object> getHints() {
		return hints;
	}

}
