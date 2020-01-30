/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.sql.ast.SqlAstSelectTranslator;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.graph.DomainResult;

import org.jboss.logging.Logger;

/**
 * Helper used to generate the SELECT for selection of an entity's identifier, here specifically intended to be used
 * as the SELECT portion of a multi-table SQM mutation
 *
 * @author Steve Ebersole
 */
public class MatchingIdSelectionHelper {
	private static final Logger log = Logger.getLogger( MatchingIdSelectionHelper.class );

	/**
	 * @asciidoc
	 *
	 * Generates a query-spec for selecting all ids matching the restriction defined as part
	 * of the user's update/delete query.  This query-spec is generally used:
	 *
	 * 		* to select all the matching ids via JDBC - see {@link MatchingIdSelectionHelper#selectMatchingIds}
	 * 		* as a sub-query restriction to insert rows into an "id table"
	 */
	public static SelectStatement generateMatchingIdSelectStatement(
			EntityMappingType targetEntityDescriptor,
			SqmDeleteOrUpdateStatement sqmStatement,
			Predicate restriction,
			MultiTableSqmMutationConverter sqmConverter,
			SessionFactoryImplementor sessionFactory) {
		final EntityDomainType entityDomainType = sqmStatement.getTarget().getModel();
		log.tracef( "Starting generation of entity-id SQM selection - %s", entityDomainType.getHibernateEntityName() );

		final QuerySpec idSelectionQuery = new QuerySpec( true, 1 );

		final TableGroup mutatingTableGroup = sqmConverter.getMutatingTableGroup();
		idSelectionQuery.getFromClause().addRoot( mutatingTableGroup );

		final List<DomainResult> domainResults = new ArrayList<>();
		final AtomicInteger i = new AtomicInteger();
		targetEntityDescriptor.getIdentifierMapping().visitColumns(
				(columnExpression, containingTableExpression, jdbcMapping) -> {
					final int position = i.getAndIncrement();
					final TableReference tableReference = mutatingTableGroup.resolveTableReference( containingTableExpression );
					final Expression expression = sqmConverter.getSqlExpressionResolver().resolveSqlExpression(
							SqlExpressionResolver.createColumnReferenceKey( tableReference, columnExpression ),
							sqlAstProcessingState -> new ColumnReference(
									tableReference,
									columnExpression,
									jdbcMapping,
									sessionFactory
							)
					);
					idSelectionQuery.getSelectClause().addSqlSelection(
							new SqlSelectionImpl(
									position,
									position + 1,
									expression,
									jdbcMapping
							)
					);

					//noinspection unchecked
					domainResults.add( new BasicResult( position, null, jdbcMapping.getJavaTypeDescriptor() ) );

				}
		);

		idSelectionQuery.applyPredicate( restriction );

		return new SelectStatement( idSelectionQuery, domainResults );
	}
	/**
	 * @asciidoc
	 *
	 * Generates a query-spec for selecting all ids matching the restriction defined as part
	 * of the user's update/delete query.  This query-spec is generally used:
	 *
	 * 		* to select all the matching ids via JDBC - see {@link MatchingIdSelectionHelper#selectMatchingIds}
	 * 		* as a sub-query restriction to insert rows into an "id table"
	 */
	public static QuerySpec generateMatchingIdSelectQuery(
			EntityMappingType targetEntityDescriptor,
			SqmDeleteOrUpdateStatement sqmStatement,
			DomainParameterXref domainParameterXref,
			Predicate restriction,
			MultiTableSqmMutationConverter sqmConverter,
			SessionFactoryImplementor sessionFactory) {
		final EntityDomainType entityDomainType = sqmStatement.getTarget().getModel();
		log.tracef( "Starting generation of entity-id SQM selection - %s", entityDomainType.getHibernateEntityName() );


		final QuerySpec idSelectionQuery = new QuerySpec( true, 1 );

		final TableGroup mutatingTableGroup = sqmConverter.getMutatingTableGroup();
		idSelectionQuery.getFromClause().addRoot( mutatingTableGroup );

		final AtomicInteger i = new AtomicInteger();
		targetEntityDescriptor.getIdentifierMapping().visitColumns(
				(columnExpression, containingTableExpression, jdbcMapping) -> {
					final int position = i.getAndIncrement();
					final TableReference tableReference = mutatingTableGroup.resolveTableReference( containingTableExpression );
					final Expression expression = sqmConverter.getSqlExpressionResolver().resolveSqlExpression(
							SqlExpressionResolver.createColumnReferenceKey( tableReference, columnExpression ),
							sqlAstProcessingState -> new ColumnReference(
									tableReference,
									columnExpression,
									jdbcMapping,
									sessionFactory
							)
					);
					idSelectionQuery.getSelectClause().addSqlSelection(
							new SqlSelectionImpl(
									position,
									position + 1,
									expression,
									jdbcMapping
							)
					);
				}
		);

		idSelectionQuery.applyPredicate( restriction );

		return idSelectionQuery;
	}

	/**
	 * Centralized selection of ids matching the restriction of the DELETE
	 * or UPDATE SQM query
	 */
	public static List<Object> selectMatchingIds(
			SqmDeleteOrUpdateStatement sqmMutationStatement,
			DomainParameterXref domainParameterXref,
			ExecutionContext executionContext) {
		final SessionFactoryImplementor factory = executionContext.getSession().getFactory();

		final EntityMappingType entityDescriptor = factory.getDomainModel()
				.getEntityDescriptor( sqmMutationStatement.getTarget().getModel().getHibernateEntityName() );

		final MultiTableSqmMutationConverter sqmConverter = new MultiTableSqmMutationConverter(
				entityDescriptor,
				domainParameterXref,
				executionContext.getQueryOptions(),
				executionContext.getQueryParameterBindings(),
				factory
		);


		final Map<SqmParameter, List<JdbcParameter>> parameterResolutions;
		if ( domainParameterXref.getSqmParameterCount() == 0 ) {
			parameterResolutions = Collections.emptyMap();
		}
		else {
			parameterResolutions = new IdentityHashMap<>();
		}

		final Predicate restriction = sqmConverter.visitWhereClause(
				sqmMutationStatement.getWhereClause(),
				columnReference -> {},
				parameterResolutions::put
		);

		final SelectStatement matchingIdSelection = generateMatchingIdSelectStatement(
				entityDescriptor,
				sqmMutationStatement,
				restriction,
				sqmConverter,
				factory
		);


		final JdbcServices jdbcServices = factory.getJdbcServices();
		final SqlAstSelectTranslator sqlAstSelectTranslator = jdbcServices.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildSelectTranslator( factory );

		final JdbcSelect idSelectJdbcOperation = sqlAstSelectTranslator.translate( matchingIdSelection );

		final JdbcParameterBindings jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
				executionContext.getQueryParameterBindings(),
				domainParameterXref,
				SqmUtil.generateJdbcParamsXref( domainParameterXref, sqmConverter ),
				factory.getDomainModel(),
				navigablePath -> sqmConverter.getMutatingTableGroup(),
				executionContext.getSession()
		);

		return jdbcServices.getJdbcSelectExecutor().list(
				idSelectJdbcOperation,
				jdbcParameterBindings,
				executionContext,
				row -> row,
				true
		);
	}
}
