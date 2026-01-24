/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.query.internal;

import jakarta.persistence.Timeout;
import org.hibernate.FlushMode;
import org.hibernate.boot.query.NamedMutationDefinition;
import org.hibernate.boot.query.NamedNativeQueryDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.query.named.NamedNativeQueryMemento;
import org.hibernate.query.named.internal.NativeMutationMementoImpl;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/// Boot-time model of a named native mutation query.
///
/// @see jakarta.persistence.NamedNativeStatement
///
/// @author Steve Ebersole
public class NamedNativeMutationDefinitionImpl<T>
		extends AbstractNamedQueryDefinition<T>
		implements NamedNativeQueryDefinition<T>, NamedMutationDefinition<T> {
	private final String sqlString;
	private final Set<String> querySpaces;

	public NamedNativeMutationDefinitionImpl(
			String name, String location,
			String sqlString, Set<String> querySpaces,
			FlushMode flushMode, Timeout timeout, String comment, Map<String, Object> hints) {
		super( name, location, flushMode, timeout, comment, hints );
		this.sqlString = sqlString;
		this.querySpaces = querySpaces;
	}

	@Override
	public String getSqlQueryString() {
		return sqlString;
	}

	@Override
	public String getResultSetMappingName() {
		return null;
	}

	@Override
	public String getStatementString() {
		return sqlString;
	}

	@Override
	public Set<String> getQuerySpaces() {
		return querySpaces;
	}

	@Override
	public NamedNativeQueryMemento<T> resolve(SessionFactoryImplementor factory) {
		return new NativeMutationMementoImpl<>(
				name,
				sqlString,
				null,
				flushMode,
				timeout,
				comment,
				hints,
				querySpaces
		);
	}

	/// Build a definition from JPA's [jakarta.persistence.NamedNativeStatement] annotation.
	///
	/// @param annotation The annotation.
	/// @param target Where the annotation was found.
	public static NamedNativeMutationDefinitionImpl<?> from(jakarta.persistence.NamedNativeStatement annotation, AnnotationTarget target) {
		return new NamedNativeMutationDefinitionImpl<>(
				annotation.name(),
				target == null ? null : target.getName(),
				annotation.statement(),
				new HashSet<>(),
				null,
				null,
				null,
				Helper.extractHints( annotation.hints() )
		);
	}
}
