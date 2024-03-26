/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Internal;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.ContributableDatabaseObject;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.model.relational.QualifiedTypeName;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;

/**
 * A mapping model object representing a relational database {@linkplain org.hibernate.annotations.Struct UDT}.
 */
public class UserDefinedType implements Serializable, ContributableDatabaseObject {

	private final String contributor;

	private Identifier catalog;
	private Identifier schema;
	private Identifier name;

	private final Map<String, Column> columns = new LinkedHashMap<>();
	private int[] orderMapping;
	private String comment;

	public UserDefinedType(
			String contributor,
			Namespace namespace,
			Identifier physicalTypeName) {
		this.contributor = contributor;
		this.catalog = namespace.getPhysicalName().getCatalog();
		this.schema = namespace.getPhysicalName().getSchema();
		this.name = physicalTypeName;
	}

	@Override
	public String getContributor() {
		return contributor;
	}

	public String getQualifiedName(SqlStringGenerationContext context) {
		return context.format( new QualifiedTypeName( catalog, schema, name ) );
	}


	public void setName(String name) {
		this.name = Identifier.toIdentifier( name );
	}

	public String getName() {
		return name == null ? null : name.getText();
	}

	public Identifier getNameIdentifier() {
		return name;
	}

	public String getQuotedName() {
		return name == null ? null : name.toString();
	}

	public String getQuotedName(Dialect dialect) {
		return name == null ? null : name.render( dialect );
	}

	public QualifiedTableName getQualifiedTableName() {
		return name == null ? null : new QualifiedTableName( catalog, schema, name );
	}

	public boolean isQuoted() {
		return name.isQuoted();
	}

	public void setQuoted(boolean quoted) {
		if ( quoted == name.isQuoted() ) {
			return;
		}
		this.name = new Identifier( name.getText(), quoted );
	}

	public void setSchema(String schema) {
		this.schema = Identifier.toIdentifier( schema );
	}

	public String getSchema() {
		return schema == null ? null : schema.getText();
	}

	public String getQuotedSchema() {
		return schema == null ? null : schema.toString();
	}

	public String getQuotedSchema(Dialect dialect) {
		return schema == null ? null : schema.render( dialect );
	}

	public boolean isSchemaQuoted() {
		return schema != null && schema.isQuoted();
	}

	public void setCatalog(String catalog) {
		this.catalog = Identifier.toIdentifier( catalog );
	}

	public String getCatalog() {
		return catalog == null ? null : catalog.getText();
	}

	public String getQuotedCatalog() {
		return catalog == null ? null : catalog.render();
	}

	public String getQuotedCatalog(Dialect dialect) {
		return catalog == null ? null : catalog.render( dialect );
	}

	public boolean isCatalogQuoted() {
		return catalog != null && catalog.isQuoted();
	}

	/**
	 * Return the column which is identified by column provided as argument.
	 *
	 * @param column column with at least a name.
	 * @return the underlying column or null if not inside this table.
	 *         Note: the instance *can* be different than the input parameter,
	 *         but the name will be the same.
	 */
	public Column getColumn(Column column) {
		if ( column == null ) {
			return null;
		}
		else {
			final Column existing = columns.get( column.getCanonicalName() );
			return column.equals( existing ) ? existing : null;
		}
	}

	public Column getColumn(Identifier name) {
		if ( name == null ) {
			return null;
		}
		return columns.get( name.getCanonicalName() );
	}

	public Column getColumn(int n) {
		final Iterator<Column> iter = columns.values().iterator();
		for ( int i = 0; i < n - 1; i++ ) {
			iter.next();
		}
		return iter.next();
	}

	public void addColumn(Column column) {
		final Column old = getColumn( column );
		if ( old == null ) {
			columns.put( column.getCanonicalName(), column );
			column.uniqueInteger = columns.size();
		}
		else {
			column.uniqueInteger = old.uniqueInteger;
		}
	}

	public int getColumnSpan() {
		return columns.size();
	}

	public Collection<Column> getColumns() {
		return columns.values();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (catalog == null ? 0 : catalog.hashCode());
		result = prime * result + (name == null ? 0 : name.hashCode());
		result = prime * result + (schema == null ? 0 : schema.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof UserDefinedType && equals( (UserDefinedType) object);
	}

	public boolean equals(UserDefinedType table) {
		if ( null == table ) {
			return false;
		}
		else if ( this == table ) {
			return true;
		}
		else {
			return Identifier.areEqual( name, table.name )
				&& Identifier.areEqual( schema, table.schema )
				&& Identifier.areEqual( catalog, table.catalog );
		}
	}

	public boolean containsColumn(Column column) {
		return columns.containsValue( column );
	}

	public String toString() {
		final StringBuilder buf = new StringBuilder()
				.append( getClass().getSimpleName() )
				.append( '(' );
		if ( getCatalog() != null ) {
			buf.append( getCatalog() ).append( "." );
		}
		if ( getSchema() != null ) {
			buf.append( getSchema() ).append( "." );
		}
		buf.append( getName() ).append( ')' );
		return buf.toString();
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	@Override
	public String getExportIdentifier() {
		final StringBuilder qualifiedName = new StringBuilder();
		if ( catalog != null ) {
			qualifiedName.append( catalog.render() ).append( '.' );
		}
		if ( schema != null ) {
			qualifiedName.append( schema.render() ).append( '.' );
		}
		return qualifiedName.append( name.render() ).toString();
	}

	@Internal
	public void reorderColumns(List<Column> columns) {
		if ( orderMapping != null ) {
			return;
		}
		orderMapping = new int[columns.size()];
		int i = 0;
		for ( Column column : this.columns.values() ) {
			orderMapping[columns.indexOf( column )] = i++;
		}
		this.columns.clear();
		for ( Column column : columns ) {
			this.columns.put( column.getCanonicalName(), column );
		}
	}

	@Internal
	public int[] getOrderMapping() {
		return orderMapping;
	}
}
