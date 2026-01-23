/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.named.internal;

import jakarta.persistence.Timeout;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.FlushMode;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.query.named.NamedQueryMemento;

import java.util.Map;

/// Base support for all types of NamedQueryMemento implementations.
///
/// @author Steve Ebersole
/// @author Gavin King
public abstract class AbstractQueryMemento<R>
		implements NamedQueryMemento<R> {
	protected final String name;

	protected final @Nullable Class<R> queryType;

	protected final FlushMode flushMode;
	protected final Timeout timeout;
	protected final String comment;

	protected final Map<String, Object> hints;

	protected AbstractQueryMemento(
			String name,
			@Nullable Class<R> queryType,
			FlushMode flushMode,
			Timeout timeout,
			String comment,
			Map<String, Object> hints) {
		this.name = name;
		this.queryType = queryType == void.class ? null : queryType;
		this.flushMode = flushMode;
		this.timeout = timeout;
		this.comment = StringHelper.nullIfEmpty( comment );
		this.hints = hints;
	}

	public AbstractQueryMemento(String name, AbstractQueryMemento<R> original) {
		this.name = name;
		this.flushMode = original.flushMode;
		this.timeout = original.timeout;
		this.comment = original.comment;
		this.hints = original.hints;
		this.queryType = original.queryType;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getRegistrationName() {
		return name;
	}

	@Override
	public FlushMode getFlushMode() {
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

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// jakarta.persistence.Refer
}
