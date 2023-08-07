/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Exportable;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.StringHelper;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.StringHelper.qualify;

/**
 * A mapping model object representing an {@linkplain jakarta.persistence.Index index} on a relational database table.
 * <p>
 * We regularize the semantics of unique constraints on nullable columns: two null values are not considered to be
 * "equal" for the purpose of determining uniqueness, just as specified by ANSI SQL and common sense.
 *
 * @author Gavin King
 */
public class Index implements Exportable, Serializable {
	private Identifier name;
	private Table table;
	private final java.util.List<Column> columns = new ArrayList<>();
	private final java.util.Map<Column, String> columnOrderMap = new HashMap<>(  );

	public static String buildSqlDropIndexString(
			String name,
			String tableName) {
		return "drop index " + qualify( tableName, name );
	}

	public static String buildSqlCreateIndexString(
			Dialect dialect,
			String name,
			String tableName,
			java.util.List<Column> columns,
			java.util.Map<Column, String> columnOrderMap,
			boolean unique) {
		StringBuilder statement = new StringBuilder( dialect.getCreateIndexString( unique ) )
				.append( " " )
				.append( dialect.qualifyIndexName() ? name : StringHelper.unqualify( name ) )
				.append( " on " )
				.append( tableName )
				.append( " (" );
		boolean first = true;
		for ( Column column : columns ) {
			if ( first ) {
				first = false;
			}
			else {
				statement.append(", ");
			}
			statement.append( column.getQuotedName( dialect ) );
			if ( columnOrderMap.containsKey( column ) ) {
				statement.append( " " ).append( columnOrderMap.get( column ) );
			}
		}
		statement.append( ")" );
		statement.append( dialect.getCreateIndexTail( unique, columns ) );

		return statement.toString();
	}

	public static String buildSqlCreateIndexString(
			SqlStringGenerationContext context,
			String name,
			Table table,
			java.util.List<Column> columns,
			java.util.Map<Column, String> columnOrderMap,
			boolean unique,
			Metadata metadata) {
		return buildSqlCreateIndexString(
				context.getDialect(),
				name,
				context.format( table.getQualifiedTableName() ),
				columns,
				columnOrderMap,
				unique
		);
	}

	public Table getTable() {
		return table;
	}

	public void setTable(Table table) {
		this.table = table;
	}

	public int getColumnSpan() {
		return columns.size();
	}

	public java.util.List<Column> getColumns() {
		return unmodifiableList( columns );
	}

	public java.util.Map<Column, String> getColumnOrderMap() {
		return unmodifiableMap( columnOrderMap );
	}

	public void addColumn(Column column) {
		if ( !columns.contains( column ) ) {
			columns.add( column );
		}
	}

	public void addColumn(Column column, String order) {
		addColumn( column );
		if ( isNotEmpty( order ) ) {
			columnOrderMap.put( column, order );
		}
	}

	public void addColumns(java.util.List<Column> extraColumns) {
		for ( Column column : extraColumns ) {
			addColumn( column );
		}
	}

	public boolean containsColumn(Column column) {
		return columns.contains( column );
	}

	public String getName() {
		return name == null ? null : name.getText();
	}

	public void setName(String name) {
		this.name = Identifier.toIdentifier( name );
	}

	public String getQuotedName(Dialect dialect) {
		return name == null ? null : name.render( dialect );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + getName() + ")";
	}

	@Override
	public String getExportIdentifier() {
		return qualify( getTable().getExportIdentifier(), "IDX-" + getName() );
	}
}
