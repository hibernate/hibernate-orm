/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.sql;
import org.hibernate.dialect.Dialect;

/**
 * An alias generator for SQL identifiers
 * @author Gavin King
 */
public final class Alias {

	private final int length;
	private final String suffix;

	/**
	 * Constructor for Alias.
	 */
	public Alias(int length, String suffix) {
		super();
		this.length = (suffix==null) ? length : length - suffix.length();
		this.suffix = suffix;
	}

	/**
	 * Constructor for Alias.
	 */
	public Alias(String suffix) {
		super();
		this.length = Integer.MAX_VALUE;
		this.suffix = suffix;
	}

	public String toAliasString(String sqlIdentifier) {
		char begin = sqlIdentifier.charAt(0);
		int quoteType = Dialect.QUOTE.indexOf(begin);
		String unquoted = getUnquotedAliasString(sqlIdentifier, quoteType);
		if ( quoteType >= 0 ) {
			char endQuote = Dialect.CLOSED_QUOTE.charAt(quoteType);
			return begin + unquoted + endQuote;
		}
		else {
			return unquoted;
		}
	}

	public String toUnquotedAliasString(String sqlIdentifier) {
		return getUnquotedAliasString(sqlIdentifier);
	}

	private String getUnquotedAliasString(String sqlIdentifier) {
		char begin = sqlIdentifier.charAt(0);
		int quoteType = Dialect.QUOTE.indexOf(begin);
		return getUnquotedAliasString(sqlIdentifier, quoteType);
	}

	private String getUnquotedAliasString(String sqlIdentifier, int quoteType) {
		String unquoted = sqlIdentifier;
		if ( quoteType >= 0 ) {
			//if the identifier is quoted, remove the quotes
			unquoted = unquoted.substring( 1, unquoted.length()-1 );
		}
		if ( unquoted.length() > length ) {
			//truncate the identifier to the max alias length, less the suffix length
			unquoted = unquoted.substring(0, length);
		}
		return ( suffix == null ) ? unquoted : unquoted + suffix;
	}

	public String[] toUnquotedAliasStrings(String[] sqlIdentifiers) {
		String[] aliases = new String[ sqlIdentifiers.length ];
		for ( int i=0; i<sqlIdentifiers.length; i++ ) {
			aliases[i] = toUnquotedAliasString(sqlIdentifiers[i]);
		}
		return aliases;
	}

	public String[] toAliasStrings(String[] sqlIdentifiers) {
		String[] aliases = new String[ sqlIdentifiers.length ];
		for ( int i=0; i<sqlIdentifiers.length; i++ ) {
			aliases[i] = toAliasString(sqlIdentifiers[i]);
		}
		return aliases;
	}

}
