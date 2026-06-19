/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.named.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Timeout;
import org.hibernate.FlushMode;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.query.named.spi.NamedQueryMemento;

import java.util.Map;

/// Base support for all types of NamedQueryMemento implementations.
///
/// @author Steve Ebersole
/// @author Gavin King
public abstract class AbstractQueryMemento<R>
		implements NamedQueryMemento<R> {
	protected final String name;

	protected final @Nullable Class<R> queryType;

	protected final @Nullable FlushMode flushMode;
	protected final @Nullable Timeout timeout;
	protected final @Nullable String comment;

	protected final Map<String, Object> hints;

	protected AbstractQueryMemento(
			@Nonnull String name,
			@Nullable Class<R> queryType,
			@Nullable FlushMode flushMode,
			@Nullable Timeout timeout,
			@Nullable String comment,
			@Nonnull Map<String, Object> hints) {
		this.name = name;
		this.queryType = queryType == void.class ? null : queryType;
		this.flushMode = flushMode;
		this.timeout = timeout;
		this.comment = StringHelper.nullIfEmpty( comment );
		this.hints = hints;
	}

	public AbstractQueryMemento(@Nonnull String name, @Nonnull AbstractQueryMemento<R> original) {
		this.name = name;
		this.flushMode = original.flushMode;
		this.timeout = original.timeout;
		this.comment = original.comment;
		this.hints = original.hints;
		this.queryType = original.queryType;
	}

	@Override
	@Nonnull
	public String getName() {
		return name;
	}

	@Nonnull
	@Override
	public String getRegistrationName() {
		return name;
	}

	@Override
	@Nullable
	public FlushMode getFlushMode() {
		return flushMode;
	}

	@Nullable
	@Override
	public Timeout getTimeout() {
		return timeout;
	}

	@Override
	@Nullable
	public String getComment() {
		return comment;
	}

	@Override
	@Nonnull
	public Map<String, Object> getHints() {
		return hints;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// jakarta.persistence.Refer
}
