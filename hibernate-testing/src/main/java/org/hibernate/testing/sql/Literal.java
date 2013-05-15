/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.testing.sql;

/**
 *
 */
public class Literal extends AbstractSqlObject {

	public static String unquoted( String text ) {
		StringBuilder builder = new StringBuilder();
		char chr;
		char prevChr = '\0';
		char quote = '\0';
		for ( int ndx = 0, len = text.length(); ndx < len; ++ndx, prevChr = chr ) {
			chr = text.charAt( ndx );
			if ( quote == '\0' ) {
				if ( chr == '\'' || chr == '"' ) {
					quote = chr;
					continue;
				}
			} else if ( chr == quote ) {
				if ( chr == '"' || prevChr != '\'' ) {
					continue;
				}
				builder.append( '\'' );
			}
			builder.append( chr );
		}
		return builder.toString();
	}

	public String text;

	Literal( SqlObject parent, String text ) {
		super( parent );
		this.text = text;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return text;
	}

	public String unquoted() {
		return unquoted( text );
	}
}
