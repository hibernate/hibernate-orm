/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.query.internal;

import jakarta.annotation.Nonnull;
import jakarta.persistence.QueryFlushMode;
import jakarta.persistence.Timeout;
import jakarta.annotation.Nullable;
import org.hibernate.boot.query.NamedQueryDefinition;

import java.util.Map;

/// Base support for all NamedQueryDefinition implementation.
///
/// @author Steve Ebersole
public abstract class AbstractNamedQueryDefinition<T> implements NamedQueryDefinition<T> {
	protected final String name;
	protected final String location;

	protected final QueryFlushMode queryFlushMode;
	protected final Timeout timeout;
	protected final String comment;
	protected final Map<String,Object> hints;

	public AbstractNamedQueryDefinition(
			@Nonnull String name,
			@Nullable String location,
			@Nullable QueryFlushMode queryFlushMode,
			@Nullable Timeout timeout,
			@Nullable String comment,
			@Nonnull Map<String, Object> hints) {
		this.name = name;
		this.location = location;
		this.queryFlushMode = queryFlushMode;
		this.timeout = timeout;
		this.comment = comment;
		this.hints = hints;
	}

	@Nonnull
	@Override
	public String getRegistrationName() {
		return name;
	}

	@Nullable
	@Override
	public QueryFlushMode getQueryFlushMode() {
		return queryFlushMode;
	}

	@Nullable
	@Override
	public Timeout getTimeout() {
		return timeout;
	}

	@Nullable
	@Override
	public String getComment() {
		return comment;
	}

	@Override
	@Nonnull
	public Map<String, Object> getHints() {
		return hints;
	}

	@Nullable
	@Override
	public String getLocation() {
		return location;
	}
}
