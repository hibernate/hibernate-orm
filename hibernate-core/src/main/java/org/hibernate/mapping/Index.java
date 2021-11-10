/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import org.hibernate.HibernateException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Exportable;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.internal.util.StringHelper;

/**
 * A relational table index
 *
 * @author Gavin King
 */
public class Index implements RelationalModel, Exportable, Serializable {
	private Table table;
	private java.util.List<Column> columns = new ArrayList<Column>();
	private java.util.Map<Column, String> columnOrderMap = new HashMap<Column, String>(  );
	private Identifier name;

	@Override
	public String sqlCreateString(Mapping mapping, SqlStringGenerationContext context, String defaultCatalog,
			String defaultSchema)
			throws HibernateException {
		Dialect dialect = context.getDialect();
		return buildSqlCreateIndexString(
				context,
				getQuotedName( dialect ),
				getTable(),
				getColumnIterator(),
				columnOrderMap,
				false,
				defaultCatalog,
				defaultSchema
		);
	}

	public static String buildSqlDropIndexString(
			SqlStringGenerationContext context,
			Table table,
			String name,
			String defaultCatalog,
			String defaultSchema) {
		return buildSqlDropIndexString( name, table.getQualifiedName( context ) );
	}

	public static String buildSqlDropIndexString(
			String name,
			String tableName) {
		return "drop index " + StringHelper.qualify( tableName, name );
	}

	public static String buildSqlCreateIndexString(
			SqlStringGenerationContext context,
			String name,
			Table table,
			Iterator<Column> columns,
			java.util.Map<Column, String> columnOrderMap,
			boolean unique,
			String defaultCatalog,
			String defaultSchema) {
		return buildSqlCreateIndexString(
				context.getDialect(),
				name,
				table.getQualifiedName( context ),
				columns,
				columnOrderMap,
				unique
		);
	}

	public static String buildSqlCreateIndexString(
			Dialect dialect,
			String name,
			String tableName,
			Iterator<Column> columns,
			java.util.Map<Column, String> columnOrderMap,
			boolean unique) {
		StringBuilder buf = new StringBuilder( "create" )
				.append( unique ? " unique" : "" )
				.append( " index " )
				.append( dialect.qualifyIndexName() ? name : StringHelper.unqualify( name ) )
				.append( " on " )
				.append( tableName )
				.append( " (" );
		while ( columns.hasNext() ) {
			Column column = columns.next();
			buf.append( column.getQuotedName( dialect ) );
			if ( columnOrderMap.containsKey( column ) ) {
				buf.append( " " ).append( columnOrderMap.get( column ) );
			}
			if ( columns.hasNext() ) {
				buf.append( ", " );
			}
		}
		buf.append( ")" );
		return buf.toString();
	}

	public static String buildSqlCreateIndexString(
			SqlStringGenerationContext context,
			String name,
			Table table,
			Iterator<Column> columns,
			java.util.Map<Column, String> columnOrderMap,
			boolean unique,
			Metadata metadata) {
		final String tableName = context.format( table.getQualifiedTableName() );

		return buildSqlCreateIndexString(
				context.getDialect(),
				name,
				tableName,
				columns,
				columnOrderMap,
				unique
		);
	}


	// Used only in Table for sqlCreateString (but commented out at the moment)
	public String sqlConstraintString(Dialect dialect) {
		StringBuilder buf = new StringBuilder( " index (" );
		Iterator iter = getColumnIterator();
		while ( iter.hasNext() ) {
			buf.append( ( (Column) iter.next() ).getQuotedName( dialect ) );
			if ( iter.hasNext() ) {
				buf.append( ", " );
			}
		}
		return buf.append( ')' ).toString();
	}

	@Override
	public String sqlDropString(SqlStringGenerationContext context,
			String defaultCatalog, String defaultSchema) {
		Dialect dialect = context.getDialect();
		return "drop index " +
				StringHelper.qualify(
						table.getQualifiedName( context ),
						getQuotedName( dialect )
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

	public Iterator<Column> getColumnIterator() {
		return columns.iterator();
	}

	public java.util.Map<Column, String> getColumnOrderMap() {
		return Collections.unmodifiableMap( columnOrderMap );
	}

	public void addColumn(Column column) {
		if ( !columns.contains( column ) ) {
			columns.add( column );
		}
	}

	public void addColumn(Column column, String order) {
		addColumn( column );
		if ( StringHelper.isNotEmpty( order ) ) {
			columnOrderMap.put( column, order );
		}
	}

	public void addColumns(Iterator extraColumns) {
		while ( extraColumns.hasNext() ) {
			addColumn( (Column) extraColumns.next() );
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
		return getClass().getName() + "(" + getName() + ")";
	}

	@Override
	public String getExportIdentifier() {
		return StringHelper.qualify( getTable().getExportIdentifier(), "IDX-" + getName() );
	}
}
