/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal;

import java.util.List;

import org.hibernate.boot.model.domain.EntityMappingHierarchy;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.metamodel.model.mapping.spi.EntityHierarchy;
import org.hibernate.metamodel.model.mapping.spi.EntityIdentifier;
import org.hibernate.query.internal.QueryHelper;
import org.hibernate.query.sqm.consume.internal.SqmConsumeHelper;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.spi.DeleteHandler;
import org.hibernate.query.sqm.mutation.spi.HandlerCreationContext;
import org.hibernate.query.sqm.mutation.spi.SqmMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.UpdateHandler;
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.predicate.SqmBetweenPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmGroupedPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmInListPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmComparisonPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmJunctivePredicate;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.consume.spi.SqlAstSelectToJdbcSelectConverter;
import org.hibernate.sql.ast.produce.sqm.spi.SqmSelectInterpretation;
import org.hibernate.sql.ast.produce.sqm.spi.SqmSelectToSqlAstConverter;
import org.hibernate.sql.exec.internal.JdbcSelectExecutorStandardImpl;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcSelect;

/**
 * @author Steve Ebersole
 */
public class SqmMutationStrategyHelper {
	/**
	 * Singleton access
	 */
	public static final SqmMutationStrategyHelper INSTANCE = new SqmMutationStrategyHelper();

	private SqmMutationStrategyHelper() {
	}

	/**
	 * Standard resolution of SqmMutationStrategy to use for a given
	 * entity hierarchy.
	 */
	public static SqmMutationStrategy resolveStrategy(
			EntityMappingHierarchy bootHierarchyDescriptor,
			EntityHierarchy entityHierarchy,
			SessionFactoryOptions options,
			ServiceRegistry serviceRegistry) {
		// todo (6.0) : Planned support for per-entity config

		if ( options.getSqmMutationStrategy() != null ) {
			return options.getSqmMutationStrategy();
		}

		return serviceRegistry.getService( JdbcServices.class )
				.getJdbcEnvironment()
				.getDialect()
				.getFallbackSqmMutationStrategy( entityHierarchy );
	}

	/**
	 * Specialized "Supplier" or "tri Function" for creating the
	 * fallback handler if the query matches no "special cases"
	 */
	public interface FallbackDeleteHandlerCreator {
		DeleteHandler create(
				SqmDeleteStatement sqmDelete,
				DomainParameterXref domainParameterXref,
				HandlerCreationContext creationContext);
	}

	/**
	 * Standard DeleteHandler resolution applying "special case" resolution
	 */
	public static DeleteHandler resolveDeleteHandler(
			SqmDeleteStatement sqmDelete,
			DomainParameterXref domainParameterXref,
			HandlerCreationContext creationContext,
			FallbackDeleteHandlerCreator fallbackCreator) {
		if ( sqmDelete.getWhereClause() == null ) {
			// special case : unrestricted
			// 		-> delete all rows, no need to use the id table
		}
		else {
			// if the predicate contains refers to any non-id Navigable, we will need to use the id table
			if ( ! hasNonIdReferences( sqmDelete.getWhereClause().getPredicate() ) ) {
				// special case : not restricted on non-id Navigable reference
				//		-> we can apply the original restriction to the individual
				//
				// todo (6.0) : technically non-id references where the reference is mapped to the primary table
				//		can also be handled by this special case.  Really the special case condition is "has
				//		any non-id references to Navigables not mapped to the primary table of the container"
			}
		}

		// otherwise, use the fallback....
		return fallbackCreator.create( sqmDelete, domainParameterXref, creationContext );
	}

	/**
	 * Specialized "Supplier" or "tri Function" for creating the
	 * fallback handler if the query mmatches no "special cases"
	 */
	public interface FallbackUpdateHandlerCreator {
		UpdateHandler create(
				SqmUpdateStatement sqmUpdate,
				DomainParameterXref domainParameterXref,
				HandlerCreationContext creationContext);
	}

	/**
	 * Standard UpdateHandler resolution applying "special case" resolution
	 */
	public static UpdateHandler resolveUpdateHandler(
			SqmUpdateStatement sqmUpdate,
			DomainParameterXref domainParameterXref,
			HandlerCreationContext creationContext,
			FallbackUpdateHandlerCreator fallbackCreator) {
		if ( sqmUpdate.getWhereClause() == null ) {
			// special case : unrestricted
			// 		-> delete all rows, no need to use the id table
		}
		else {
			// see if the predicate contains any non-id Navigable references
			if ( ! hasNonIdReferences( sqmUpdate.getWhereClause().getPredicate() ) ) {
				// special case : not restricted on non-id Navigable reference
				//		-> we can apply the original restriction to the individual updates without needing to use the id-table
				//
				// todo (6.0) : technically non-id references where the reference is mapped to the primary table
				//		can also be handled by this special case.  Really the special case condition is "has
				//		any non-id references to Navigables not mapped to the primary table of the container"
			}
		}

		// todo (6.0) : implement the above special cases

		// otherwise, use the fallback....
		return fallbackCreator.create( sqmUpdate, domainParameterXref, creationContext );
	}

	/**
	 * Does the given `predicate` "non-identifier Navigable references"?
	 *
	 * @see #isNonIdentifierReference
	 */
	@SuppressWarnings("WeakerAccess")
	public static boolean hasNonIdReferences(SqmPredicate predicate) {
		if ( predicate instanceof SqmGroupedPredicate ) {
			return hasNonIdReferences( ( (SqmGroupedPredicate) predicate ).getSubPredicate() );
		}

		if ( predicate instanceof SqmJunctivePredicate ) {
			return hasNonIdReferences( ( (SqmJunctivePredicate) predicate ).getLeftHandPredicate() )
					&& hasNonIdReferences( ( (SqmJunctivePredicate) predicate ).getRightHandPredicate() );
		}

		if ( predicate instanceof SqmComparisonPredicate ) {
			final SqmExpression lhs = ( (SqmComparisonPredicate) predicate ).getLeftHandExpression();
			final SqmExpression rhs = ( (SqmComparisonPredicate) predicate ).getRightHandExpression();

			return isNonIdentifierReference( lhs ) || isNonIdentifierReference( rhs );
		}

		if ( predicate instanceof SqmInListPredicate ) {
			final SqmInListPredicate<?> inPredicate = (SqmInListPredicate) predicate;
			if ( isNonIdentifierReference( inPredicate.getTestExpression() ) ) {
				return true;
			}

			for ( SqmExpression listExpression : inPredicate.getListExpressions() ) {
				if ( isNonIdentifierReference( listExpression ) ) {
					return true;
				}
			}

			return false;
		}

		if ( predicate instanceof SqmBetweenPredicate ) {
			final SqmBetweenPredicate betweenPredicate = (SqmBetweenPredicate) predicate;
			return isNonIdentifierReference( betweenPredicate.getExpression() )
					|| isNonIdentifierReference( betweenPredicate.getLowerBound() )
					|| isNonIdentifierReference( betweenPredicate.getUpperBound() );
		}

		return false;
	}

	/**
	 * Is the given `expression` a `SqmNavigableReference` that is also a reference
	 * to a non-`EntityIdentifier` `Navigable`?
	 *
	 * @see SqmNavigableReference
	 * @see EntityIdentifier
	 */
	@SuppressWarnings("WeakerAccess")
	public static boolean isNonIdentifierReference(SqmExpression expression) {
		if ( expression instanceof SqmNavigableReference ) {
			return ! EntityIdentifier.class.isInstance( expression );
		}

		return false;
	}

	/**
	 * Centralized selection of ids matching the restriction of the DELETE
	 * or UPDATE SQM query
	 */
	public static List<Object> selectMatchingIds(
			DomainParameterXref domainParameterXref,
			SqmDeleteOrUpdateStatement sqmDeleteStatement,
			ExecutionContext executionContext) {
		final SqmQuerySpec sqmIdSelectQuerySpec = SqmIdSelectGenerator.generateSqmEntityIdSelect(
				sqmDeleteStatement,
				executionContext.getSession().getSessionFactory()
		);

		final SqmSelectToSqlAstConverter sqmConverter = new SqmSelectToSqlAstConverter(
				executionContext.getQueryOptions(),
				domainParameterXref,
				executionContext.getDomainParameterBindingContext().getQueryParameterBindings(),
				executionContext.getLoadQueryInfluencers(),
				executionContext.getCallback(),
				executionContext.getSession().getSessionFactory()
		);

		final SqmSelectStatement sqmIdSelect = new SqmSelectStatement(
				executionContext.getSession().getFactory().getQueryEngine().getCriteriaBuilder()
		);

		sqmIdSelect.setQuerySpec( sqmIdSelectQuerySpec );

		final SqmSelectInterpretation sqmSelectInterpretation = sqmConverter.interpret( sqmIdSelect );

		final JdbcSelect jdbcSelect = SqlAstSelectToJdbcSelectConverter.interpret(
				sqmSelectInterpretation,
				executionContext.getSession().getSessionFactory()
		);

		return JdbcSelectExecutorStandardImpl.INSTANCE.list(
				jdbcSelect,
				QueryHelper.buildJdbcParameterBindings(
						domainParameterXref,
						SqmConsumeHelper.generateJdbcParamsXref( domainParameterXref, sqmConverter ),
						executionContext
				),
				executionContext,
				row -> row[0]
		);
	}
}
