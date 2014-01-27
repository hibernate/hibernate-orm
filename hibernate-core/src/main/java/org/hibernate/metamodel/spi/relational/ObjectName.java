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
package org.hibernate.metamodel.spi.relational;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.StringHelper;

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
	private final Identifier catalog;
	private final Identifier schema;
	private final Identifier name;

	private final String identifier;
	private final int hashCode;

	/**
	 * Tries to create an {@code ObjectName} from a name.  This form explicitly looks for the form
	 * {@code catalog.schema.name}.  If you need db specific parsing use
	 * {@link org.hibernate.engine.jdbc.env.spi.QualifiedObjectNameSupport#parseName} instead
	 *
	 * @param text simple or qualified name of the database object.
	 */
	public static ObjectName parse(String text) {
		if ( text == null ) {
			throw new IllegalIdentifierException( "Object name must be specified" );
		}

		String schemaName = null;
		String catalogName = null;
		String localObjectName;

		boolean wasQuoted = false;
		if ( text.startsWith( "`" ) && text.endsWith( "`" ) ) {
			wasQuoted = true;
			text = StringHelper.unquote( text );
		}

		final String[] tokens = text.split( "." );
		if ( tokens.length == 0 || tokens.length == 1 ) {
			// we have just a local name...
			localObjectName = text;
		}
		else if ( tokens.length == 2 ) {
			schemaName = tokens[0];
			localObjectName = tokens[1];
		}
		else if ( tokens.length == 3 ) {
			schemaName = tokens[0];
			catalogName = tokens[1];
			localObjectName = tokens[2];
		}
		else {
			throw new HibernateException( "Unable to parse object name: " + text );
		}

		final Identifier schema = Identifier.toIdentifier( schemaName, wasQuoted );
		final Identifier catalog = Identifier.toIdentifier( catalogName, wasQuoted );
		final Identifier object = Identifier.toIdentifier( localObjectName, wasQuoted );
		return new ObjectName( catalog, schema, object );
	}

	public ObjectName(Identifier name) {
		this( null, null, name );
	}

	public ObjectName(Schema schema, String name) {
		this( schema.getName().getCatalog(), schema.getName().getSchema(), Identifier.toIdentifier( name ) );
	}

	public ObjectName(Schema schema, Identifier name) {
		this( schema.getName().getCatalog(), schema.getName().getSchema(), name );
	}

	public ObjectName(Schema.Name schemaName, Identifier name) {
		this( schemaName.getCatalog(), schemaName.getSchema(), name );
	}

	public ObjectName(String catalogName, String schemaName, String name) {
		this(
				Identifier.toIdentifier( catalogName ),
				Identifier.toIdentifier( schemaName ),
				Identifier.toIdentifier( name )
		);
	}

	/**
	 * Creates a qualified name reference.
	 *
	 * @param catalog The catalog in which the object is defined (optional)
	 * @param schema The in which the object is defined (optional)
	 * @param name The name (required)
	 */
	public ObjectName(Identifier catalog, Identifier schema, Identifier name) {
		if ( name == null ) {
			// Identifier cannot be constructed with an 'empty' name
			throw new IllegalIdentifierException( "Object name must be specified" );
		}
		this.catalog = catalog;
		this.schema = schema;
		this.name = name;

		this.identifier = qualify(
				catalog == null ? null : catalog.toString(),
				schema == null ? null : schema.toString(),
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
				encloseInQuotesIfQuoted( catalog, dialect ),
				encloseInQuotesIfQuoted( schema, dialect ),
				encloseInQuotesIfQuoted( name, dialect )
		);
	}

	private static String encloseInQuotesIfQuoted(Identifier identifier, Dialect dialect) {
		return identifier == null ?
				null :
				identifier.getText( dialect );
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
				"catalog='" + catalog + '\'' +
				", schema='" + schema + '\'' +
				", name='" + name + '\'' +
				'}';
	}

	private boolean areEqual(Identifier one, Identifier other) {
		return one == null
				? other == null
				: one.equals( other );
	}
}

