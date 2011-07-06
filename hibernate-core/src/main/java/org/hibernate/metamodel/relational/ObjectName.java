/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.relational;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;

/**
 * Models the qualified name of a database object.
 * <p/>
 * Some things to keep in mind wrt catalog/schema:
 * 1) {@link java.sql.DatabaseMetaData#isCatalogAtStart}
 * 2) {@link java.sql.DatabaseMetaData#getCatalogSeparator()}
 *
 * @author Steve Ebersole
 */
public class ObjectName {
	// todo - should depend on DatabaseMetaData. For now hard coded (HF)
	private static String SEPARATOR = ".";

	private final Identifier schema;
	private final Identifier catalog;
	private final Identifier name;

	private final String identifier;
	private final int hashCode;

	/**
	 * Tries to create an {@code ObjectName} from a name.
	 *
	 * @param objectName simple or qualified name of the database object.
	 */
	public ObjectName(String objectName) {
		this(
				extractSchema( objectName ),
				extractCatalog( objectName ),
				extractName( objectName )
		);
	}

	public ObjectName(Identifier name) {
		this( null, null, name );
	}

	public ObjectName(Schema schema, String name) {
		this( schema.getName().getSchema(), schema.getName().getCatalog(), Identifier.toIdentifier( name ) );
	}

	public ObjectName(Schema schema, Identifier name) {
		this( schema.getName().getSchema(), schema.getName().getCatalog(), name );
	}

	public ObjectName(String schemaName, String catalogName, String name) {
		this(
				Identifier.toIdentifier( schemaName ),
				Identifier.toIdentifier( catalogName ),
				Identifier.toIdentifier( name )
		);
	}

	/**
	 * Creates a qualified name reference.
	 *
	 * @param schema The in which the object is defined (optional)
	 * @param catalog The catalog in which the object is defined (optional)
	 * @param name The name (required)
	 */
	public ObjectName(Identifier schema, Identifier catalog, Identifier name) {
		if ( name == null ) {
			// Identifier cannot be constructed with an 'empty' name
			throw new IllegalIdentifierException( "Object name must be specified" );
		}
		this.name = name;
		this.schema = schema;
		this.catalog = catalog;

		this.identifier = qualify(
				schema == null ? null : schema.toString(),
				catalog == null ? null : catalog.toString(),
				name.toString()
		);

		int tmpHashCode = schema != null ? schema.hashCode() : 0;
		tmpHashCode = 31 * tmpHashCode + ( catalog != null ? catalog.hashCode() : 0 );
		tmpHashCode = 31 * tmpHashCode + name.hashCode();
		this.hashCode = tmpHashCode;
	}

	public Identifier getSchema() {
		return schema;
	}

	public Identifier getCatalog() {
		return catalog;
	}

	public Identifier getName() {
		return name;
	}

	public String toText() {
		return identifier;
	}

	public String toText(Dialect dialect) {
		if ( dialect == null ) {
			throw new IllegalArgumentException( "dialect must be non-null." );
		}
		return qualify(
				encloseInQuotesIfQuoted( schema, dialect ),
				encloseInQuotesIfQuoted( catalog, dialect ),
				encloseInQuotesIfQuoted( name, dialect )
		);
	}

	private static String encloseInQuotesIfQuoted(Identifier identifier, Dialect dialect) {
		return identifier == null ?
				null :
				identifier.encloseInQuotesIfQuoted( dialect );
	}

	private static String qualify(String schema, String catalog, String name) {
		StringBuilder buff = new StringBuilder( name );
		if ( catalog != null ) {
			buff.insert( 0, catalog + '.' );
		}
		if ( schema != null ) {
			buff.insert( 0, schema + '.' );
		}
		return buff.toString();
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		ObjectName that = (ObjectName) o;

		return name.equals( that.name )
				&& areEqual( catalog, that.catalog )
				&& areEqual( schema, that.schema );
	}

	@Override
	public int hashCode() {
		return hashCode;
	}


	@Override
	public String toString() {
		return "ObjectName{" +
				"name='" + name + '\'' +
				", schema='" + schema + '\'' +
				", catalog='" + catalog + '\'' +
				'}';
	}

	private boolean areEqual(Identifier one, Identifier other) {
		return one == null
				? other == null
				: one.equals( other );
	}

	private static String extractSchema(String qualifiedName) {
		if ( qualifiedName == null ) {
			return null;
		}
		String[] tokens = qualifiedName.split( SEPARATOR );
		if ( tokens.length == 0 || tokens.length == 1 ) {
			return null;
		}
		else if ( tokens.length == 2 ) {
			// todo - this case needs to be refined w/ help of  DatabaseMetaData (HF)
			return null;
		}
		else if ( tokens.length == 3 ) {
			return tokens[0];
		}
		else {
			throw new HibernateException( "Unable to parse object name: " + qualifiedName );
		}
	}

	private static String extractCatalog(String qualifiedName) {
		if ( qualifiedName == null ) {
			return null;
		}
		String[] tokens = qualifiedName.split( SEPARATOR );
		if ( tokens.length == 0 || tokens.length == 1 ) {
			return null;
		}
		else if ( tokens.length == 2 ) {
			// todo - this case needs to be refined w/ help of  DatabaseMetaData (HF)
			return null;
		}
		else if ( tokens.length == 3 ) {
			return tokens[1];
		}
		else {
			throw new HibernateException( "Unable to parse object name: " + qualifiedName );
		}
	}

	private static String extractName(String qualifiedName) {
		if ( qualifiedName == null ) {
			return null;
		}
		String[] tokens = qualifiedName.split( SEPARATOR );
		if ( tokens.length == 0 ) {
			return qualifiedName;
		}
		else {
			return tokens[tokens.length - 1];
		}
	}
}

