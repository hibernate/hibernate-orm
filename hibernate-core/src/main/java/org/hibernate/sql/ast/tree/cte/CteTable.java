/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.cte;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.Bindable;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.from.StandardTableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.results.internal.SqlSelectionImpl;

/**
 * Describes the table definition for the CTE - its name amd its columns
 *
 * @author Steve Ebersole
 */
public class CteTable {
	private final SessionFactoryImplementor sessionFactory;
	private final String cteName;
	private final List<CteColumn> cteColumns;

	public CteTable(String cteName, EntityMappingType entityDescriptor) {
		final int numberOfColumns = entityDescriptor.getIdentifierMapping().getJdbcTypeCount();
		final List<CteColumn> columns = new ArrayList<>( numberOfColumns );
		entityDescriptor.getIdentifierMapping().forEachSelectable(
				(columnIndex, selection) -> columns.add(
						new CteColumn("cte_" + selection.getSelectionExpression(), selection.getJdbcMapping() )
				)
		);
		this.cteName = cteName;
		this.cteColumns = columns;
		this.sessionFactory = entityDescriptor.getEntityPersister().getFactory();
	}

	public CteTable(String cteName, List<CteColumn> cteColumns, SessionFactoryImplementor sessionFactory) {
		this.cteName = cteName;
		this.cteColumns = cteColumns;
		this.sessionFactory = sessionFactory;
	}

	public String getTableExpression() {
		return cteName;
	}

	public List<CteColumn> getCteColumns() {
		return cteColumns;
	}

	public QuerySpec createCteDefinition(
			List<?> matchingIds,
			Bindable bindable,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext) {
		final QuerySpec querySpec = new QuerySpec( false );

		final TableReference tableValueConstructorReference = createCteDefinitionTableValueCtor(
				matchingIds,
				bindable,
				jdbcParameterBindings,
				executionContext
		);

		final StandardTableGroup tableValueCtorGroup = new StandardTableGroup(
				true,
				new NavigablePath( "cte" ),
				null,
				null,
				tableValueConstructorReference,
				null,
				sessionFactory
		);

		querySpec.getFromClause().addRoot( tableValueCtorGroup );

		applySelections( querySpec, tableValueConstructorReference );

		return querySpec;
	}

	private TableReference createCteDefinitionTableValueCtor(
			List<?> matchingValues,
			Bindable bindable,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext) {
		// use `DerivedTable` as the TableValueConstructor
		//		so its `#expression` would be something like `values ( (a1, b1), (a2, b2), ... )`

		final int numberOfColumns = getCteColumns().size();

		final StringBuilder tableValueCtorExpressionBuffer = new StringBuilder( "values(" );
		final List<JdbcParameter> jdbcParameters = Arrays.asList( new JdbcParameterImpl[numberOfColumns] );
		String rowSeparator = "";
		for ( Object matchingId : matchingValues ) {
			tableValueCtorExpressionBuffer.append( rowSeparator );

			char separator = '(';
			for ( int i = 0; i < numberOfColumns; i++ ) {
				tableValueCtorExpressionBuffer.append( separator );
				tableValueCtorExpressionBuffer.append( '?' );
				separator = ',';
				jdbcParameters.set( i, new JdbcParameterImpl( cteColumns.get( i ).getJdbcMapping() ) );
			}
			tableValueCtorExpressionBuffer.append( ')' );

			jdbcParameterBindings.registerParametersForEachJdbcValue(
					matchingId,
					Clause.IRRELEVANT,
					bindable,
					jdbcParameters,
					executionContext.getSession()
			);

			rowSeparator = ", ";
		}

		tableValueCtorExpressionBuffer.append( ')' );

		return new TableReference(
				tableValueCtorExpressionBuffer.toString(),
				cteName,
				false,
				sessionFactory
		);
	}

	public QuerySpec createCteSubQuery(@SuppressWarnings("unused") ExecutionContext executionContext) {
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
							// todo (6.0) : handle read/write transformers for the CTE columns
							new ColumnReference(
									tableReference,
									cteColumn.getColumnExpression(),
									false,
									null,
									null,
									cteColumn.getJdbcMapping(),
									sessionFactory
							)
					)
			);
		}
	}
}
