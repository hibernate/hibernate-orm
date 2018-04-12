/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.ordering.antlr;

import org.hibernate.NullPrecedence;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.ast.util.ASTPrinter;
import org.hibernate.hql.internal.ast.util.TokenPrinters;
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

	private final SessionFactoryImplementor sessionFactory;

	public OrderByFragmentRenderer(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	protected void out(AST ast) {
		out( ( (Node) ast ).getRenderableText() );
	}


	// handle trace logging ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private int traceDepth = 0;

	@Override
	public void traceIn(String ruleName, AST tree) {
		if ( inputState.guessing > 0 ) {
			return;
		}
		String prefix = StringHelper.repeat( '-', ( traceDepth++ * 2 ) ) + "-> ";
		String traceText = ruleName + " (" + buildTraceNodeName( tree ) + ")";
		LOG.trace( prefix + traceText );
	}

	private String buildTraceNodeName(AST tree) {
		return tree == null
				? "???"
				: tree.getText() + " [" + TokenPrinters.ORDERBY_FRAGMENT_PRINTER.getTokenTypeName( tree.getType() ) + "]";
	}

	@Override
	public void traceOut(String ruleName, AST tree) {
		if ( inputState.guessing > 0 ) {
			return;
		}
		String prefix = "<-" + StringHelper.repeat( '-', ( --traceDepth * 2 ) ) + " ";
		LOG.trace( prefix + ruleName );
	}

	@Override
	protected String renderOrderByElement(String expression, String collation, String order, String nulls) {
		final NullPrecedence nullPrecedence = NullPrecedence.parse(
				nulls,
				sessionFactory.getSessionFactoryOptions().getDefaultNullPrecedence()
		);
		return sessionFactory.getDialect().renderOrderByElement( expression, collation, order, nullPrecedence );
	}
}
