/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import jakarta.annotation.Nullable;

import jakarta.annotation.Nonnull;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.internal.SimpleNaturalIdMapping;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.JdbcParameter;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.predicate.NullnessPredicate;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;
import org.hibernate.sql.exec.spi.JdbcParameterBinding;

/**
 * NaturalIdLoader for simple natural-ids
 */
public class SimpleNaturalIdLoader<T> extends AbstractNaturalIdLoader<T> {

	public SimpleNaturalIdLoader(
			@Nonnull SimpleNaturalIdMapping naturalIdMapping,
			@Nonnull EntityMappingType entityDescriptor) {
		super( naturalIdMapping, entityDescriptor );
	}

	@Nonnull
	@Override
	protected SimpleNaturalIdMapping naturalIdMapping() {
		return (SimpleNaturalIdMapping) super.naturalIdMapping();
	}

	@Override
	protected void applyNaturalIdRestriction(
			@Nullable Object bindValue,
			@Nonnull TableGroup rootTableGroup,
			@Nonnull Consumer<Predicate> predicateConsumer,
			@Nonnull BiConsumer<JdbcParameter, JdbcParameterBinding> jdbcParameterConsumer,
			@Nonnull LoaderSqlAstCreationState sqlAstCreationState,
			@Nonnull SharedSessionContractImplementor session) {
		final var expressionResolver = sqlAstCreationState.getSqlExpressionResolver();
		final var naturalIdMapping = naturalIdMapping().getAttribute();
		if ( bindValue == null ) {
			naturalIdMapping.forEachSelectable(
					(index, selectable) -> {
						final Expression columnReference =
								resolveColumnReference( rootTableGroup, selectable, expressionResolver );
						predicateConsumer.accept( new NullnessPredicate( columnReference ) );
					}
			);
		}
		else {
			naturalIdMapping.breakDownJdbcValues(
					bindValue,
					(valueIndex, jdbcValue, jdbcValueMapping) ->
							applyRestriction(
									rootTableGroup,
									predicateConsumer,
									jdbcParameterConsumer,
									jdbcValue,
									jdbcValueMapping,
									expressionResolver
							),
					session
			);
		}
	}
}
