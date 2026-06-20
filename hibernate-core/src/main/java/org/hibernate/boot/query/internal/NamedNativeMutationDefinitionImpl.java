/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.query.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.NamedNativeStatement;
import jakarta.persistence.QueryFlushMode;
import jakarta.persistence.Timeout;
import org.hibernate.boot.query.NamedMutationDefinition;
import org.hibernate.boot.query.NamedNativeQueryDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.query.named.spi.NamedNativeQueryMemento;
import org.hibernate.query.named.internal.NativeMutationMementoImpl;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/// Boot-time model of a named native mutation query.
///
/// @see NamedNativeStatement
///
/// @author Steve Ebersole
public class NamedNativeMutationDefinitionImpl<T>
		extends AbstractNamedQueryDefinition<T>
		implements NamedNativeQueryDefinition<T>, NamedMutationDefinition<T> {
	private final String sqlString;
	private final Set<String> querySpaces;

	public NamedNativeMutationDefinitionImpl(
			@Nonnull String name,
			@Nullable String location,
			@Nonnull String sqlString,
			@Nonnull Set<String> querySpaces,
			@Nullable QueryFlushMode queryFlushMode,
			@Nullable Timeout timeout,
			@Nullable String comment,
			@Nonnull Map<String, Object> hints) {
		super( name, location, queryFlushMode, timeout, comment, hints );
		this.sqlString = sqlString;
		this.querySpaces = querySpaces;
	}

	@Nonnull
	@Override
	public String getSqlQueryString() {
		return sqlString;
	}

	@Nullable
	@Override
	public String getResultSetMappingName() {
		return null;
	}

	@Nonnull
	@Override
	public String getStatementString() {
		return sqlString;
	}

	@Nullable
	@Override
	public Set<String> getQuerySpaces() {
		return querySpaces;
	}

	@Nonnull
	@Override
	public NamedNativeQueryMemento<T> resolve(@Nonnull SessionFactoryImplementor factory) {
		return new NativeMutationMementoImpl<>(
				name,
				sqlString,
				null,
				queryFlushMode,
				timeout,
				comment,
				hints,
				querySpaces
		);
	}

	/// Build a definition from JPA's [NamedNativeStatement] annotation.
	///
	/// @param annotation The annotation.
	/// @param target Where the annotation was found.
	@Nonnull
	public static NamedNativeMutationDefinitionImpl<?> from(
			@Nonnull NamedNativeStatement annotation,
			@Nullable AnnotationTarget target) {
		return new NamedNativeMutationDefinitionImpl<>(
				annotation.name(),
				target == null ? null : target.getName(),
				annotation.statement(),
				new HashSet<>(),
				QueryFlushMode.DEFAULT,
				null,
				null,
				Helper.extractHints( annotation.hints() )
		);
	}
}
