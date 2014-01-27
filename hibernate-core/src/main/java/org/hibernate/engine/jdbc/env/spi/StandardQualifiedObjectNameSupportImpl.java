/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.engine.jdbc.env.spi;

import java.util.regex.Pattern;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.spi.relational.IllegalIdentifierException;
import org.hibernate.metamodel.spi.relational.ObjectName;

/**
 * @author Steve Ebersole
 */
public class StandardQualifiedObjectNameSupportImpl implements QualifiedObjectNameSupport {
	public static final char DEFAULT_QUOTE_START = '\'';
	public static final char DEFAULT_QUOTE_END = '\'';
	public static final String DEFAULT_CATALOG_SEPARATOR = ".";

	private final String catalogSeparator;
	private final boolean catalogAfterName;
	private final char quotedStart;
	private final char quotedEnd;

	private final Pattern splitPattern;

	public StandardQualifiedObjectNameSupportImpl(
			String catalogSeparator,
			boolean catalogAfterName,
			char quotedStart,
			char quotedEnd) {
		this.catalogSeparator = catalogSeparator;
		this.catalogAfterName = catalogAfterName;
		this.quotedStart = quotedStart;
		this.quotedEnd = quotedEnd;

		splitPattern =  ".".equals( catalogSeparator )
				? Pattern.compile( Pattern.quote( "." ) )
				: Pattern.compile( "[\\." + catalogSeparator + "]" );
	}

	public StandardQualifiedObjectNameSupportImpl() {
		this( DEFAULT_CATALOG_SEPARATOR, false, DEFAULT_QUOTE_START, DEFAULT_QUOTE_END );
	}

	public StandardQualifiedObjectNameSupportImpl(String catalogSeparator, boolean catalogAfterName, Dialect dialect) {
		this( catalogSeparator, catalogAfterName, dialect.openQuote(), dialect.closeQuote() );
	}

	public StandardQualifiedObjectNameSupportImpl(Dialect dialect) {
		this( DEFAULT_CATALOG_SEPARATOR, false, dialect );
	}

	public StandardQualifiedObjectNameSupportImpl(String catalogSeparator, boolean catalogAfterName) {
		this( catalogSeparator, catalogAfterName, DEFAULT_QUOTE_START, DEFAULT_QUOTE_END );
	}


	@Override
	public String formatName(ObjectName objectName) {
		StringBuilder buffer = new StringBuilder();
		if ( ! catalogAfterName ) {
			if ( objectName.getCatalog() != null ) {
				buffer.append( objectName.getCatalog().getText( quotedStart, quotedEnd ) )
						.append( catalogSeparator );
			}
		}

		if ( objectName.getSchema() != null ) {
			buffer.append( objectName.getSchema().getText( quotedStart, quotedEnd ) ).append( '.' );
		}
		buffer.append( objectName.getName().getText( quotedStart, quotedEnd ) );

		if ( catalogAfterName ) {
			if ( objectName.getCatalog() != null ) {
				buffer.append( catalogSeparator )
						.append( objectName.getCatalog().getText( quotedStart, quotedEnd ) );
			}
		}

		return buffer.toString();
	}

	@Override
	public ObjectName parseName(String text) {
		if ( text == null ) {
			throw new IllegalIdentifierException( "Object name must be specified" );
		}

		String catalogName = null;
		String schemaName = null;
		String localObjectName;

		final String[] tokens = splitPattern.split( text );
		if ( tokens.length == 0 || tokens.length == 1 ) {
			// we have just a local name...
			localObjectName = text;
		}
		else if ( tokens.length == 2 ) {
			// we have 'something.name', no real way to know if something is a catalog or schema
			// but thats ok based on the way we implement Database... so assume schema
			schemaName = tokens[0];
			localObjectName = tokens[1];
		}
		else if ( tokens.length == 3 ) {
			if ( catalogAfterName ) {
				schemaName = tokens[0];
				localObjectName = tokens[1];
				catalogName = tokens[2];
			}
			else {
				catalogName = tokens[0];
				schemaName = tokens[1];
				localObjectName = tokens[2];
			}
		}
		else {
			throw new HibernateException( "Unable to parse object name: " + text );
		}

		return new ObjectName( catalogName, schemaName, localObjectName );
	}
}
