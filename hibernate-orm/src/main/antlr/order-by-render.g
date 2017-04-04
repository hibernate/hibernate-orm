header
{
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
}
/**
 * Antlr grammar for rendering <tt>ORDER_BY</tt> trees as described by the {@link OrderByFragmentParser}

 * @author Steve Ebersole
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
class GeneratedOrderByFragmentRenderer extends TreeParser;

options {
	importVocab=OrderByTemplate;
	buildAST=false;
}

{
    // the buffer to which we write the resulting SQL.
	private StringBuilder buffer = new StringBuilder();

	protected void out(String text) {
	    buffer.append( text );
	}

	protected void out(AST ast) {
	    buffer.append( ast.getText() );
	}

    /*package*/ String getRenderedFragment() {
        return buffer.toString();
    }

	/**
	 * Implementation note: This is just a stub. OrderByFragmentRenderer contains the effective implementation.
	 */
	protected String renderOrderByElement(String expression, String collation, String order, String nulls) {
		throw new UnsupportedOperationException("Concrete ORDER BY renderer should override this method.");
	}
}

orderByFragment
    : #(
        ORDER_BY sortSpecification ( {out(", ");} sortSpecification)*
    )
    ;

sortSpecification { String sortKeySpec = null; String collSpec = null; String ordSpec = null; String nullOrd = null; }
    : #(
        SORT_SPEC sortKeySpec=sortKeySpecification (collSpec=collationSpecification)? (ordSpec=orderingSpecification)? (nullOrd=nullOrdering)?
            { out( renderOrderByElement( sortKeySpec, collSpec, ordSpec, nullOrd ) ); }
    )
    ;

sortKeySpecification returns [String sortKeyExp = null]
    : #(SORT_KEY s:sortKey) { sortKeyExp = #s.getText(); }
    ;

sortKey
    : IDENT
    ;

collationSpecification returns [String collSpecExp = null]
    : c:COLLATE { collSpecExp = "collate " + #c.getText(); }
    ;

orderingSpecification returns [String ordSpecExp = null]
    : o:ORDER_SPEC { ordSpecExp = #o.getText(); }
    ;

nullOrdering returns [String nullOrdExp = null]
    : n:NULL_ORDER { nullOrdExp = #n.getText(); }
    ;