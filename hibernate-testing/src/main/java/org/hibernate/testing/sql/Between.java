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
public class Between extends Operation {

	Between( SqlObject parent, String operator, int precedence ) {
		super( parent, operator, precedence );
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if ( operands.isEmpty() ) {
			builder.append( operator );
		} else {
			builder.append( operands.get( 0 ) ).append( ' ' ).append( operator );
			if ( operands.size() > 1 ) {
				builder.append( ' ' ).append( operands.get( 1 ) );
			}
			if ( operands.size() > 2 ) {
				builder.append( " AND " ).append( operands.get( 2 ) );
			}
		}
		return builder.toString();
	}
}
