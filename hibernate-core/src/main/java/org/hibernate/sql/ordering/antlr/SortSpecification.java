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
package org.hibernate.sql.ordering.antlr;

import antlr.collections.AST;

/**
 * Models each sorting exprersion.
 *
 * @author Steve Ebersole
 */
public class SortSpecification extends NodeSupport {
	/**
	 * Locate the specified {@link SortKey}.
	 *
	 * @return The sort key.
	 */
	public SortKey getSortKey() {
		return ( SortKey ) getFirstChild();
	}

	/**
	 * Locate the specified <tt>collation specification</tt>, if one.
	 *
	 * @return The <tt>collation specification</tt>, or null if none was specified.
	 */
	public CollationSpecification getCollation() {
		AST possible = getSortKey().getNextSibling();
		return  possible != null && OrderByTemplateTokenTypes.COLLATE == possible.getType()
				? ( CollationSpecification ) possible
				: null;
	}

	/**
	 * Locate the specified <tt>ordering specification</tt>, if one.
	 *
	 * @return The <tt>ordering specification</tt>, or null if none was specified.
	 */
	public OrderingSpecification getOrdering() {
		// IMPL NOTE : the ordering-spec would be either the 2nd or 3rd child (of the overall sort-spec), if it existed,
		// 		depending on whether a collation-spec was specified.

		AST possible = getSortKey().getNextSibling();
		if ( possible == null ) {
			// There was no sort-spec parts specified other then the sort-key so there can be no ordering-spec...
			return null;
		}

		if ( OrderByTemplateTokenTypes.COLLATE == possible.getType() ) {
			// the 2nd child was a collation-spec, so we need to check the 3rd child instead.
			possible = possible.getNextSibling();
		}

		return possible != null && OrderByTemplateTokenTypes.ORDER_SPEC == possible.getType()
				?  ( OrderingSpecification ) possible
				:  null;
	}
}
