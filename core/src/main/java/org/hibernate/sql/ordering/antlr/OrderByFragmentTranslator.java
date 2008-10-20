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

import java.io.StringReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.HibernateException;
import org.hibernate.hql.ast.util.ASTPrinter;

/**
 * A translator which coordinates translation of an <tt>order-by</tt> mapping.
 *
 * @author Steve Ebersole
 */
public class OrderByFragmentTranslator {
	private static final Logger log = LoggerFactory.getLogger( OrderByFragmentTranslator.class );

	public final TranslationContext context;

	public OrderByFragmentTranslator(TranslationContext context) {
		this.context = context;
	}

	/**
	 * The main contract, performing the transaction.
	 *
	 * @param fragment The <tt>order-by</tt> mapping fragment to be translated.
	 *
	 * @return The translated fragment.
	 */
	public String render(String fragment) {
		GeneratedOrderByLexer lexer = new GeneratedOrderByLexer( new StringReader( fragment ) );
		OrderByFragmentParser parser = new OrderByFragmentParser( lexer, context );
		try {
			parser.orderByFragment();
		}
		catch ( HibernateException e ) {
			throw e;
		}
		catch ( Throwable t ) {
			throw new HibernateException( "Unable to parse order-by fragment", t );
		}

		if ( log.isTraceEnabled() ) {
			ASTPrinter printer = new ASTPrinter( OrderByTemplateTokenTypes.class );
			log.trace( printer.showAsString( parser.getAST(), "--- {order-by fragment} ---" ) );
		}

		GeneratedOrderByFragmentRenderer renderer = new GeneratedOrderByFragmentRenderer();
		try {
			renderer.orderByFragment( parser.getAST() );
		}
		catch ( HibernateException e ) {
			throw e;
		}
		catch ( Throwable t ) {
			throw new HibernateException( "Unable to render parsed order-by fragment", t );
		}

		return renderer.getRenderedFragment();
	}
}
