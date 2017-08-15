/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.ordering.internal;

import java.util.List;
import java.util.Locale;

import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.model.relational.spi.PhysicalColumn;
import org.hibernate.metamodel.model.relational.spi.PhysicalTable;

/**
 * @author Steve Ebersole
 */
public class OrderByTranslationImpl implements OrderByTranslation {
	private final String sqlFragmentTemplate;
	private final List<PhysicalColumn> referencedColumns;

	public OrderByTranslationImpl(
			String sqlFragmentTemplate,
			List<PhysicalColumn> referencedColumns) {
		this.sqlFragmentTemplate = sqlFragmentTemplate;
		this.referencedColumns = referencedColumns;
	}

	@Override
	public String injectAliases(OrderByAliasResolver aliasResolver) {
		final Dialect dialect = aliasResolver.getSessionFactory().getJdbcServices().getJdbcEnvironment().getDialect();

		String sql = sqlFragmentTemplate;

		for ( PhysicalColumn referencedColumn : referencedColumns ) {
			final String placeholderText;

			final String tableAlias = aliasResolver.resolveTableAlias( referencedColumn );

			if ( PhysicalTable.class.isInstance( referencedColumn.getSourceTable() ) ) {
				final PhysicalTable physicalTable = (PhysicalTable) referencedColumn.getSourceTable();
				placeholderText = determinePhysicalTablePlaceholderText( physicalTable, referencedColumn );
			}
			else {
				placeholderText = determineVirtualTablePlaceholderText( referencedColumn );
			}

			final String replacementText;
			final String renderedColumnName = referencedColumn.getName().render( dialect );
			if ( tableAlias != null ) {
				replacementText = tableAlias + '.' + renderedColumnName;
			}
			else {
				replacementText = renderedColumnName;
			}

			sql = sql.replace( placeholderText, replacementText );
		}

		return sql;
	}

	public static String determinePlaceholderText(PhysicalColumn column) {
		if ( PhysicalTable.class.isInstance( column.getSourceTable() ) ) {
			return determinePhysicalTablePlaceholderText( (PhysicalTable) column.getSourceTable(), column );
		}
		else {
			return determineVirtualTablePlaceholderText( column );
		}
	}

	private static String determinePhysicalTablePlaceholderText(PhysicalTable table, PhysicalColumn column) {
		return String.format(
				Locale.ROOT,
				"{%s.%s}",
				table.getTableName().getText(),
				column.getName().getText()
		);
	}

	private static String determineVirtualTablePlaceholderText(PhysicalColumn column) {
		return '{' + column.getName().getText() + '}';
	}
}
