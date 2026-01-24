/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.query.internal;

import jakarta.persistence.Timeout;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.FlushMode;
import org.hibernate.boot.query.NamedQueryDefinition;

import java.util.Map;

/// Base support for all NamedQueryDefinition implementation.
///
/// @author Steve Ebersole
public abstract class AbstractNamedQueryDefinition<T> implements NamedQueryDefinition<T> {
	protected final String name;
	protected final String location;

	protected final FlushMode flushMode;
	protected final Timeout timeout;
	protected final String comment;
	protected final Map<String,Object> hints;

	public AbstractNamedQueryDefinition(
			String name, String location,
			FlushMode flushMode, Timeout timeout, String comment, Map<String, Object> hints) {
		this.name = name;
		this.location = location;
		this.flushMode = flushMode;
		this.timeout = timeout;
		this.comment = comment;
		this.hints = hints;
	}

	@Override
	public String getRegistrationName() {
		return name;
	}

	@Override
	public FlushMode getQueryFlushMode() {
		return flushMode;
	}

	@Override
	public Timeout getTimeout() {
		return timeout;
	}

	@Override
	public String getComment() {
		return comment;
	}

	@Override
	public Map<String, Object> getHints() {
		return hints;
	}

	@Override
	public @Nullable String getLocation() {
		return location;
	}
}
