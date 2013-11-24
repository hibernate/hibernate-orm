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

import org.hibernate.NullPrecedence;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.ast.util.ASTPrinter;
import org.hibernate.internal.util.StringHelper;

import org.jboss.logging.Logger;

import antlr.collections.AST;

/**
 * Extension of the Antlr-generated tree walker for rendering the parsed order-by tree back to String form.
 * {@link #out(antlr.collections.AST)} is the sole semantic action here and it is used to utilize our
 * split between text (tree debugging text) and "renderable text" (text to use during rendering).
 *
 * @author Steve Ebersole
 */
public class OrderByFragmentRenderer extends GeneratedOrderByFragmentRenderer {

	private static final Logger LOG = Logger.getLogger( OrderByFragmentRenderer.class.getName() );
	private static final ASTPrinter printer = new ASTPrinter( GeneratedOrderByFragmentRendererTokenTypes.class );

	private final SessionFactoryImplementor sessionFactory;

	public OrderByFragmentRenderer(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
    protected void out(AST ast) {
		out( ( ( Node ) ast ).getRenderableText() );
	}


	// handle trace logging ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    private int traceDepth = 0;

	@Override
    public void traceIn(String ruleName, AST tree) {
		if ( inputState.guessing > 0 ) {
			return;
		}
		String prefix = StringHelper.repeat( '-', (traceDepth++ * 2) ) + "-> ";
		String traceText = ruleName + " (" + buildTraceNodeName(tree) + ")";
		LOG.trace( prefix + traceText );
	}

	private String buildTraceNodeName(AST tree) {
		return tree == null
				? "???"
				: tree.getText() + " [" + printer.getTokenTypeName( tree.getType() ) + "]";
	}

	@Override
    public void traceOut(String ruleName, AST tree) {
		if ( inputState.guessing > 0 ) {
			return;
		}
		String prefix = "<-" + StringHelper.repeat( '-', (--traceDepth * 2) ) + " ";
		LOG.trace( prefix + ruleName );
	}

	@Override
	protected String renderOrderByElement(String expression, String collation, String order, String nulls) {
		final NullPrecedence nullPrecedence = NullPrecedence.parse( nulls, sessionFactory.getSettings().getDefaultNullPrecedence() );
		return sessionFactory.getDialect().renderOrderByElement( expression, collation, order, nullPrecedence );
	}
}
