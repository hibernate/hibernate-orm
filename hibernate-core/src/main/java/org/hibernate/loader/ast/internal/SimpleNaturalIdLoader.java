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
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.NullnessPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
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
		if ( bindValue == null ) {
			naturalIdMapping().getAttribute().forEachSelectable(
					(index, selectable) -> {
						final Expression columnReference = resolveColumnReference(
								rootTableGroup,
								selectable,
								sqlAstCreationState.getSqlExpressionResolver(),
								session.getFactory()
						);
						predicateConsumer.accept( new NullnessPredicate( columnReference ) );
					}
			);
		}
		else {
			naturalIdMapping().getAttribute().breakDownJdbcValues(
					bindValue,
					(valueIndex, jdbcValue, jdbcValueMapping) -> {
						final Expression columnReference = resolveColumnReference(
								rootTableGroup,
								jdbcValueMapping,
								sqlAstCreationState.getSqlExpressionResolver(),
								session.getFactory()
						);
						if ( jdbcValue == null ) {
							predicateConsumer.accept( new NullnessPredicate( columnReference ) );
						}
						else {
							final JdbcParameter jdbcParameter = new JdbcParameterImpl( jdbcValueMapping.getJdbcMapping() );
							final ComparisonPredicate predicate = new ComparisonPredicate(
									columnReference,
									ComparisonOperator.EQUAL,
									jdbcParameter
							);
							predicateConsumer.accept( predicate );
							jdbcParameterConsumer.accept(
									jdbcParameter,
									new JdbcParameterBindingImpl( jdbcValueMapping.getJdbcMapping(), jdbcValue )
							);
						}
					},
					session
			);
		}
	}
}
