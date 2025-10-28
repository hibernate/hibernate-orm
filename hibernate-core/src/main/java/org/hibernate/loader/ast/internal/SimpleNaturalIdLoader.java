/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.internal.SimpleNaturalIdMapping;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.predicate.NullnessPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.exec.spi.JdbcParameterBinding;

/**
 * NaturalIdLoader for simple natural-ids
 */
public class SimpleNaturalIdLoader<T> extends AbstractNaturalIdLoader<T> {

	public SimpleNaturalIdLoader(
			SimpleNaturalIdMapping naturalIdMapping,
			EntityMappingType entityDescriptor) {
		super( naturalIdMapping, entityDescriptor );
	}

	@Override
	protected SimpleNaturalIdMapping naturalIdMapping() {
		return (SimpleNaturalIdMapping) super.naturalIdMapping();
	}

	@Override
	protected void applyNaturalIdRestriction(
			Object bindValue,
			TableGroup rootTableGroup,
			Consumer<Predicate> predicateConsumer,
			BiConsumer<JdbcParameter, JdbcParameterBinding> jdbcParameterConsumer,
			LoaderSqlAstCreationState sqlAstCreationState,
			SharedSessionContractImplementor session) {
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
