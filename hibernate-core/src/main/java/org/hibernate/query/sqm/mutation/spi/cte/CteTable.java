/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.spi.cte;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.hibernate.LockMode;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.model.mapping.EntityTypeDescriptor;
import org.hibernate.metamodel.model.mapping.spi.Writeable;
import org.hibernate.metamodel.model.relational.spi.AbstractTable;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.DerivedTable;
import org.hibernate.metamodel.model.relational.spi.PhysicalColumn;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.tree.from.StandardTableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.exec.internal.StandardJdbcParameterImpl;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBinding;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.results.internal.SqlSelectionImpl;

/**
 * Adaption of the CTE expression as a {@link org.hibernate.metamodel.model.relational.spi.Table}
 * to be used in SQL AST
 *
 * @author Steve Ebersole
 */
public class CteTable extends AbstractTable {
	private final EntityTypeDescriptor<?> entityDescriptor;

	public CteTable(EntityTypeDescriptor<?> entityDescriptor) {
		super( UUID.randomUUID(), false );
		this.entityDescriptor = entityDescriptor;

		for ( Column column : (List<Column>) entityDescriptor.getIdentifierDescriptor().getColumns() ) {
			final CteTableColumn cteTableColumn = new CteTableColumn(
					this,
					(PhysicalColumn) column,
					entityDescriptor.getTypeConfiguration()
			);

			addColumn( cteTableColumn );
		}
	}

	@Override
	public String getTableExpression() {
		return CteBasedMutationStrategy.ID_CTE;
	}

	@Override
	public String render(Dialect dialect, JdbcEnvironment jdbcEnvironment) {
		return getTableExpression();
	}

	@Override
	public boolean isExportable() {
		return false;
	}

	@Override
	public String toLoggableFragment() {
		return getTableExpression();
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
				new NavigablePath( entityDescriptor.getIdentifierDescriptor().getNavigableRole().getFullPath() ),
				entityDescriptor.getIdentifierDescriptor(),
				LockMode.NONE,
				tableValueConstructorReference,
				Collections.emptyList()
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

		final int numberOfColumns = getColumns().size();

		final StringBuilder tableValueCtorExpressionBuffer = new StringBuilder( "values(" );
		String rowSeparator = "";
		int idProcessedCount = 0;
		for ( Object matchingId : matchingIds ) {
			tableValueCtorExpressionBuffer.append( rowSeparator );

			tableValueCtorExpressionBuffer.append( '(' );
			StringHelper.repeat( "?", numberOfColumns, ",", tableValueCtorExpressionBuffer );
			tableValueCtorExpressionBuffer.append( ')' );

			final int currentIdPosition = idProcessedCount;

			entityDescriptor.getIdentifierDescriptor().dehydrate(
					matchingId,
					new Writeable.JdbcValueCollector() {
						int currentColumnPosition = 0;

						@Override
						public void collect(
								Object jdbcValue,
								SqlExpressableType type,
								Column boundColumn) {
							jdbcParameterBindings.addBinding(
									new StandardJdbcParameterImpl(
											currentIdPosition + currentColumnPosition++,
											type,
											// it is effectively the where-clause
											Clause.WHERE,
											executionContext.getSession().getFactory().getTypeConfiguration()
									),
									new JdbcParameterBinding() {
										@Override
										public SqlExpressableType getBindType() {
											return type;
										}

										@Override
										public Object getBindValue() {
											return jdbcValue;
										}
									}
							);
						}
					},
					Clause.WHERE,
					executionContext.getSession()
			);


			rowSeparator = ", ";
			idProcessedCount++;
		}
		tableValueCtorExpressionBuffer.append( ')' );

		final DerivedTable derivedTable = new DerivedTable( null, tableValueCtorExpressionBuffer.toString(), true );

		return new TableReference(
				derivedTable,
				CteBasedMutationStrategy.ID_CTE,
				false
		);
	}

	public QuerySpec createCteSubQuery(ExecutionContext executionContext) {
		final QuerySpec querySpec = new QuerySpec( false );

		final CteTableReference cteTableReference = new CteTableReference( this, executionContext );
		final CteTableGroup cteTableGroup = new CteTableGroup( entityDescriptor, cteTableReference );
		querySpec.getFromClause().addRoot( cteTableGroup );

		applySelections( querySpec, cteTableReference );

		return querySpec;
	}

	private void applySelections(QuerySpec querySpec, TableReference tableReference) {
		int i = 0;
		for ( Column column : getColumns() ) {
			querySpec.getSelectClause().addSqlSelection(
					new SqlSelectionImpl(
							i + 1,
							i,
							tableReference.resolveColumnReference( column ),
							column.getExpressableType()
					)
			);
		}
	}
}
