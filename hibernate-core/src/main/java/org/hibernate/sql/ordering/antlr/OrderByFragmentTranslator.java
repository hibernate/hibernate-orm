/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008 Red Hat Inc. or third-party contributors as
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
package org.hibernate.sql.ordering.antlr;

import java.io.StringReader;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.hql.internal.ast.util.ASTPrinter;

import org.jboss.logging.Logger;

/**
 * A translator for order-by mappings, whether specified by hbm.xml files, Hibernate
 * {@link org.hibernate.annotations.OrderBy} annotation or JPA {@link javax.persistence.OrderBy} annotation.
 *
 * @author Steve Ebersole
 */
public class OrderByFragmentTranslator {
	private static final Logger LOG = Logger.getLogger( OrderByFragmentTranslator.class.getName() );

	/**
	 * Perform the translation of the user-supplied fragment, returning the translation.
	 * <p/>
	 * The important distinction to this split between (1) translating and (2) resolving aliases is that
	 * both happen at different times
	 *
	 *
	 * @param context Context giving access to delegates needed during translation.
	 * @param fragment The user-supplied order-by fragment
	 *
	 * @return The translation.
	 */
	public static OrderByTranslation translate(TranslationContext context, String fragment) {
		GeneratedOrderByLexer lexer = new GeneratedOrderByLexer( new StringReader( fragment ) );

		// Perform the parsing (and some analysis/resolution).  Another important aspect is the collection
		// of "column references" which are important later to seek out replacement points in the
		// translated fragment.
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

		if ( LOG.isTraceEnabled() ) {
			ASTPrinter printer = new ASTPrinter( OrderByTemplateTokenTypes.class );
			LOG.trace( printer.showAsString( parser.getAST(), "--- {order-by fragment} ---" ) );
		}

		// Render the parsed tree to text.
		OrderByFragmentRenderer renderer = new OrderByFragmentRenderer( context.getSessionFactory() );
		try {
			renderer.orderByFragment( parser.getAST() );
		}
		catch ( HibernateException e ) {
			throw e;
		}
		catch ( Throwable t ) {
			throw new HibernateException( "Unable to render parsed order-by fragment", t );
		}

		return new StandardOrderByTranslationImpl( renderer.getRenderedFragment(), parser.getColumnReferences() );
	}

	public static class StandardOrderByTranslationImpl implements OrderByTranslation {
		private final String sqlTemplate;
		private final Set<String> columnReferences;

		public StandardOrderByTranslationImpl(String sqlTemplate, Set<String> columnReferences) {
			this.sqlTemplate = sqlTemplate;
			this.columnReferences = columnReferences;
		}

		@Override
		public String injectAliases(OrderByAliasResolver aliasResolver) {
			String sql = sqlTemplate;
			for ( String columnReference : columnReferences ) {
				final String replacementToken = "{" + columnReference + "}";
				sql = sql.replace(
						replacementToken,
						aliasResolver.resolveTableAlias( columnReference ) + '.' + columnReference
				);
			}
			return sql;
		}
	}
}
