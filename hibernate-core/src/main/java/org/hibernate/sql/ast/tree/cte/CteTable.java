/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.cte;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.mutation.internal.cte.CteBasedMutationStrategy;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.StandardTableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameter;
import org.hibernate.sql.exec.spi.JdbcParameterBinding;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.results.internal.SqlSelectionImpl;

/**
 * Describes the CTE and exposes ways to consume it
 *
 * @author Steve Ebersole
 */
public class CteTable {
	private final EntityMappingType entityDescriptor;
	private final SessionFactoryImplementor sessionFactory;

	private final List<CteColumn> cteColumns;

	public CteTable(EntityMappingType entityDescriptor, RuntimeModelCreationContext runtimeModelCreationContext) {
		this.entityDescriptor = entityDescriptor;
		this.sessionFactory = entityDescriptor.getEntityPersister().getFactory();

		final int numberOfColumns = entityDescriptor.getIdentifierMapping().getJdbcTypeCount( runtimeModelCreationContext.getTypeConfiguration() );
		cteColumns = new ArrayList<>( numberOfColumns );
		entityDescriptor.getIdentifierMapping().visitColumns(
				(columnExpression, containingTableExpression, jdbcMapping) -> cteColumns.add(
						new CteColumn(
								this,
								"cte_" + columnExpression,
								jdbcMapping
						)
				)
		);
	}

	public String getTableExpression() {
		return CteBasedMutationStrategy.TABLE_NAME;
	}

	public List<CteColumn> getCteColumns() {
		return cteColumns;
	}

	public QuerySpec createCteDefinition(
			List<?> matchingIds,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext) {
		final QuerySpec querySpec = new QuerySpec( false );

		final TableReference tableValueConstructorReference = createCteDefinitionTableValueCtor(
				matchingIds,
				jdbcParameterBindings,
				executionContext
		);

		final StandardTableGroup tableValueCtorGroup = new StandardTableGroup(
				new NavigablePath( "cte" ),
				null,
				LockMode.NONE,
				tableValueConstructorReference,
				Collections.emptyList(),
				null,
				sessionFactory
		);

		querySpec.getFromClause().addRoot( tableValueCtorGroup );

		applySelections( querySpec, tableValueConstructorReference );

		return querySpec;
	}

	private TableReference createCteDefinitionTableValueCtor(
			List<?> matchingIds,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext) {
		// use `DerivedTable` as the TableValueConstructor
		//		so its `#expression` would be something like `values ( (a1, b1), (a2, b2), ... )`

		final int numberOfColumns = getCteColumns().size();

		final StringBuilder tableValueCtorExpressionBuffer = new StringBuilder( "values(" );
		String rowSeparator = "";
		int idProcessedCount = 0;
		for ( Object matchingId : matchingIds ) {
			tableValueCtorExpressionBuffer.append( rowSeparator );

			tableValueCtorExpressionBuffer.append( '(' );
			StringHelper.repeat( "?", numberOfColumns, ",", tableValueCtorExpressionBuffer );
			tableValueCtorExpressionBuffer.append( ')' );

			final int currentIdPosition = idProcessedCount;

			entityDescriptor.getIdentifierMapping().visitJdbcValues(
					matchingId,
					Clause.IRRELEVANT,
					(value, type) -> {
						final JdbcParameter jdbcParameter = new JdbcParameterImpl( type );
						JdbcParameterBinding jdbcParameterBinding = new JdbcParameterBinding() {
							@Override
							public JdbcMapping getBindType() {
								return type;
							}

							@Override
							public Object getBindValue() {
								return value;
							}
						};
						jdbcParameterBindings.addBinding(
								jdbcParameter,
								jdbcParameterBinding
						);
					},
					executionContext.getSession()
			);

			rowSeparator = ", ";
			idProcessedCount++;
		}

		tableValueCtorExpressionBuffer.append( ')' );

		return new TableReference(
				tableValueCtorExpressionBuffer.toString(),
				CteBasedMutationStrategy.TABLE_NAME,
				false,
				sessionFactory
		);
	}

	public QuerySpec createCteSubQuery(ExecutionContext executionContext) {
		final QuerySpec querySpec = new QuerySpec( false );

		final TableReference cteTableReference = new TableReference(
				getTableExpression(),
				null,
				false,
				sessionFactory
		);

		final CteTableGroup cteTableGroup = new CteTableGroup( cteTableReference );
		querySpec.getFromClause().addRoot( cteTableGroup );

		applySelections( querySpec, cteTableReference );

		return querySpec;
	}

	private void applySelections(QuerySpec querySpec, TableReference tableReference) {
		for ( int i = 0; i < cteColumns.size(); i++ ) {
			final CteColumn cteColumn = cteColumns.get( i );
			querySpec.getSelectClause().addSqlSelection(
					new SqlSelectionImpl(
							i + 1,
							i,
							new ColumnReference(
									tableReference,
									cteColumn.getColumnExpression(),
									cteColumn.getJdbcMapping(),
									sessionFactory
							),
							cteColumn.getJdbcMapping()
					)
			);
		}
	}
}
