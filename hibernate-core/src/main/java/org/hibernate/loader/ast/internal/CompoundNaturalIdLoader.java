package org.hibernate.loader.ast.internal;

import jakarta.annotation.Nullable;

import jakarta.annotation.Nonnull;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.internal.CompoundNaturalIdMapping;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.JdbcParameter;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.predicate.NullnessPredicate;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;
import org.hibernate.sql.exec.spi.JdbcParameterBinding;

/**
 * NaturalIdLoader implementation for compound natural-ids
 */
public class CompoundNaturalIdLoader<T> extends AbstractNaturalIdLoader<T> {

	public CompoundNaturalIdLoader(
			@Nonnull CompoundNaturalIdMapping naturalIdMapping,
			@Nonnull EntityMappingType entityDescriptor) {
		super( naturalIdMapping, entityDescriptor );
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
		if ( bindValue == null ) {
			for ( var naturalIdAttribute : naturalIdMapping().getNaturalIdAttributes() ) {
				naturalIdAttribute.forEachSelectable(
						(index, selectable) -> {
							final Expression columnReference =
									resolveColumnReference( rootTableGroup, selectable, expressionResolver );
							predicateConsumer.accept( new NullnessPredicate( columnReference ) );
						}
				);
			}
		}
		else {
			naturalIdMapping().breakDownJdbcValues(
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
