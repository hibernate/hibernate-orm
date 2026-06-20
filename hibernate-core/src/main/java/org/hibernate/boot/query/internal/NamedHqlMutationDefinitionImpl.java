/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.query.internal;

import jakarta.persistence.NamedStatement;
import jakarta.persistence.QueryFlushMode;
import jakarta.persistence.Timeout;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.boot.query.NamedHqlQueryDefinition;
import org.hibernate.boot.query.NamedMutationDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.query.named.spi.NamedSqmQueryMemento;
import org.hibernate.query.named.internal.HqlMutationMementoImpl;

import java.util.Map;

/**
 * Models a {@linkplain NamedStatement}.
 *
 * @author Steve Ebersole
 */
public class NamedHqlMutationDefinitionImpl<T>
	extends AbstractNamedQueryDefinition<T>
		implements NamedHqlQueryDefinition<T>, NamedMutationDefinition<T> {
	private final @Nonnull String hql;
	private final @Nullable Class<T> targetType;

	public NamedHqlMutationDefinitionImpl(
			@Nonnull String name,
			@Nullable String location,
			@Nonnull String hql,
			@Nullable Class<T> targetType,
			@Nullable QueryFlushMode queryFlushMode,
			@Nullable Timeout timeout,
			@Nullable String comment,
			@Nonnull Map<String, Object> hints) {
		super( name, location, queryFlushMode, timeout, comment, hints );
		this.hql = hql;
		this.targetType = targetType;
	}

	@Nonnull
	public String getHqlString() {
		return hql;
	}

	@Nonnull
	@Override
	public String getStatementString() {
		return getHqlString();
	}

	@Nonnull
	@Override
	public NamedSqmQueryMemento<T> resolve(@Nonnull SessionFactoryImplementor factory) {
		return new HqlMutationMementoImpl<>(
				getRegistrationName(),
				hql, targetType, Map.of(),
				queryFlushMode, timeout, comment, hints
		);
	}


	/// Build a definition from JPA's [NamedStatement] annotation.
	///
	/// @param annotation The annotation.
	/// @param target Where the annotation was found.
	@Nonnull
	public static NamedHqlMutationDefinitionImpl<?> from(
			@Nonnull NamedStatement annotation,
			@Nullable AnnotationTarget target) {
		return new NamedHqlMutationDefinitionImpl<>(
				annotation.name(),
				target == null ? null : target.getName(),
				annotation.statement(),
				null,
				null,
				null,
				null,
				Helper.extractHints( annotation.hints() )
		);
	}

}
