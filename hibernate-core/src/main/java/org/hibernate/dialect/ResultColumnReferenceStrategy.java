/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.dialect;

/**
 * Defines how we need to reference columns in the group-by, having, and order-by
 * clauses.
 *
 * @author Steve Ebersole
 */
public enum ResultColumnReferenceStrategy {
	/**
	 * This strategy says to reference the result columns by the qualified column name
	 * found in the result source.  This strategy is not strictly allowed by ANSI SQL
	 * but is Hibernate's legacy behavior and is also the fastest of the strategies; thus
	 * it should be used if supported by the underlying database.
	 */
	SOURCE,
	/**
	 * For databases which do not support {@link #SOURCE}, ANSI SQL defines two allowable
	 * approaches.  One is to reference the result column by the alias it is given in the
	 * result source (if it is given an alias).  This strategy says to use this approach.
	 * <p/>
	 * The other QNSI SQL compliant approach is {@link #ORDINAL}.
	 */
	ALIAS,
	/**
	 * For databases which do not support {@link #SOURCE}, ANSI SQL defines two allowable
	 * approaches.  One is to reference the result column by the ordinal position at which
	 * it appears in the result source.  This strategy says to use this approach.
	 * <p/>
	 * The other QNSI SQL compliant approach is {@link #ALIAS}.
	 */
	ORDINAL;

	/**
	 * Resolves the strategy by name, in a case insensitive manner.  If the name cannot be resolved, {@link #SOURCE}
	 * is returned as the default.
	 *
	 * @param name The strategy name to resolve
	 *
	 * @return The resolved strategy
	 */
	public static ResultColumnReferenceStrategy resolveByName(String name) {
		if ( ALIAS.name().equalsIgnoreCase( name ) ) {
			return ALIAS;
		}
		else if ( ORDINAL.name().equalsIgnoreCase( name ) ) {
			return ORDINAL;
		}
		else {
			return SOURCE;
		}
	}
}
