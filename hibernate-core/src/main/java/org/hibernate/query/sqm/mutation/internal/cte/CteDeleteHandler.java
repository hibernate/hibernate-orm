/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.cte;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.ColumnConsumer;
import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.internal.SqmMutationStrategyHelper;
import org.hibernate.query.sqm.mutation.spi.DeleteHandler;
import org.hibernate.query.sqm.mutation.spi.HandlerCreationContext;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.resource.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.ast.tree.cte.CteTable;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcDelete;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;

/**
 * Bulk-id delete handler that uses CTE and VALUES lists.
 *
 * @author Evandro Pires da Silva
 * @author Vlad Mihalcea
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public class CteDeleteHandler extends AbstractCteMutationHandler implements DeleteHandler {
	private final SqlAstTranslatorFactory sqlAstTranslatorFactory;

	protected CteDeleteHandler(
			CteTable cteTable,
			SqmDeleteStatement sqmDeleteStatement,
			DomainParameterXref domainParameterXref,
			CteBasedMutationStrategy strategy,
			HandlerCreationContext creationContext) {
		super( cteTable, sqmDeleteStatement, domainParameterXref, strategy, creationContext );

		final SessionFactoryImplementor sessionFactory = creationContext.getSessionFactory();
		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		sqlAstTranslatorFactory = jdbcEnvironment.getSqlAstTranslatorFactory();
	}

	@Override
	public SqmDeleteStatement getSqmDeleteOrUpdateStatement() {
		return (SqmDeleteStatement) super.getSqmDeleteOrUpdateStatement();
	}

	@Override
	public int execute(ExecutionContext executionContext) {
		final List<Object> ids = selectMatchingIds( executionContext );
		if ( ids == null || ids.isEmpty() ) {
			return 0;
		}

		final QuerySpec cteQuerySpec = getCteTable().createCteSubQuery( executionContext );

		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl( getDomainParameterXref().getQueryParameterCount() );
		final QuerySpec cteDefinitionQuerySpec = getCteTable().createCteDefinition(
				ids,
				jdbcParameterBindings,
				executionContext
		);

		// for every table to be deleted, create the CteStatement and execute it

		getEntityDescriptor().visitAttributeMappings(
				attribute -> {
					if ( attribute instanceof PluralAttributeMapping ) {
						final PluralAttributeMapping pluralAttribute = (PluralAttributeMapping) attribute;

						if ( pluralAttribute.getSeparateCollectionTable() != null ) {
							// this collection has a separate collection table, meaning it is one of:
							//		1) element-collection
							//		2) many-to-many
							//		3) one-to many using a dedicated join-table
							//
							// in all of these cases, we should clean up the matching rows in the
							// collection table

							executeDelete(
									cteDefinitionQuerySpec,
									pluralAttribute.getSeparateCollectionTable(),
									() -> columnConsumer -> pluralAttribute.getKeyDescriptor().visitReferringColumns( columnConsumer ),
									pluralAttribute.getKeyDescriptor(),
									cteQuerySpec,
									jdbcParameterBindings,
									executionContext
							);
						}
					}
				}
		);

		getEntityDescriptor().visitConstraintOrderedTables(
				(tableExpression, tableColumnsVisitationSupplier) -> {
					executeDelete(
							cteDefinitionQuerySpec,
							tableExpression,
							tableColumnsVisitationSupplier,
							getEntityDescriptor().getIdentifierMapping(),
							cteQuerySpec,
							jdbcParameterBindings,
							executionContext
					);
				}
		);

		return ids.size();
	}

	private List<Object> selectMatchingIds(ExecutionContext executionContext) {
		return SqmMutationStrategyHelper.selectMatchingIds(
				getSqmDeleteOrUpdateStatement(),
				getDomainParameterXref(),
				executionContext
		);
	}

	protected void executeDelete(
			QuerySpec cteDefinition,
			String targetTable,
			Supplier<Consumer<ColumnConsumer>> columnsToMatchVisitationSupplier,
			MappingModelExpressable cteType,
			QuerySpec cteSubQuery,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext) {
		final CteStatement cteStatement = generateCteStatement(
				cteDefinition,
				targetTable,
				columnsToMatchVisitationSupplier,
				cteType,
				cteSubQuery,
				executionContext
		);

		final SessionFactoryImplementor sessionFactory = getCreationContext().getSessionFactory();

		final JdbcDelete jdbcDelete = sqlAstTranslatorFactory.buildDeleteTranslator( sessionFactory )
				.translate( cteStatement );


		final LogicalConnectionImplementor logicalConnection = executionContext.getSession()
				.getJdbcCoordinator()
				.getLogicalConnection();

		sessionFactory.getJdbcServices().getJdbcDeleteExecutor().execute(
				jdbcDelete,
				jdbcParameterBindings,
				sql -> {
					try {
						return logicalConnection.getPhysicalConnection().prepareStatement( sql );
					}
					catch (SQLException e) {
						throw sessionFactory.getJdbcServices().getSqlExceptionHelper().convert(
								e,
								"Error performing DELETE",
								sql
						);
					}
				},
				(integer, preparedStatement) -> {},
				executionContext
		);
	}

	protected CteStatement generateCteStatement(
			QuerySpec cteDefinition,
			String targetTable,
			Supplier<Consumer<ColumnConsumer>> columnsToMatchVisitationSupplier,
			MappingModelExpressable cteType,
			QuerySpec cteSubQuery,
			ExecutionContext executionContext) {
		final DeleteStatement deleteStatement = generateCteConsumer(
				targetTable,
				columnsToMatchVisitationSupplier,
				cteType,
				cteSubQuery,
				executionContext
		);
		return new CteStatement(
				cteDefinition,
				CteBasedMutationStrategy.TABLE_NAME,
				getCteTable(),
				deleteStatement
		);
	}


	private DeleteStatement generateCteConsumer(
			String targetTable,
			Supplier<Consumer<ColumnConsumer>> columnsToMatchVisitationSupplier,
			MappingModelExpressable cteType,
			QuerySpec cteSubQuery,
			ExecutionContext executionContext) {
		final SessionFactoryImplementor sessionFactory = executionContext.getSession().getFactory();
		final TableReference targetTableReference = new TableReference(
				targetTable,
				null,
				false,
				sessionFactory
		);

		final List<ColumnReference> columnsToMatchReferences = new ArrayList<>();

		columnsToMatchVisitationSupplier.get().accept(
				(columnExpression, containingTableExpression, jdbcMapping) -> {
					columnsToMatchReferences.add(
							new ColumnReference(
									targetTableReference,
									columnExpression,
									jdbcMapping,
									sessionFactory
							)
					);
				}
		);

		final Expression columnsToMatchExpression;

		if ( columnsToMatchReferences.size() == 1 ) {
			columnsToMatchExpression = columnsToMatchReferences.get( 0 );
		}
		else {
			columnsToMatchExpression = new SqlTuple( columnsToMatchReferences, cteType );
		}

		return new DeleteStatement(
				targetTableReference,
				new InSubQueryPredicate(
						columnsToMatchExpression,
						cteSubQuery,
						false
				)
		);
	}
}
