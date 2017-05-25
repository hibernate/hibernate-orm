/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function.spi;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.produce.function.internal.PatternRenderer;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.spi.expression.Expression;

/**
 * Represents HQL functions that can have different representations in different SQL dialects where that
 * difference can be handled via a template/pattern.
 * <p/>
 * E.g. in HQL we can define function <code>concat(?1, ?2)</code> to concatenate two strings
 * p1 and p2.  Dialects would register different versions of this class *using the same name* (concat) but with
 * different templates or patterns; <code>(?1 || ?2)</code> for Oracle, <code>concat(?1, ?2)</code> for MySql,
 * <code>(?1 + ?2)</code> for MS SQL.  Each dialect will define a template as a string (exactly like above) marking function
 * parameters with '?' followed by parameter's index (first index is 1).
 *
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 */
public class PatternBasedSqmFunctionTemplate extends AbstractSelfRenderingFunctionTemplate implements SelfRenderingFunctionSupport {
	private final PatternRenderer renderer;
	private final AllowableFunctionReturnType type;
	private final boolean hasParenthesesIfNoArgs;

	/**
	 * Constructs a SQLFunctionTemplate
	 *
	 * @param type The functions return type
	 * @param template The function template
	 */
	public PatternBasedSqmFunctionTemplate(AllowableFunctionReturnType type, String template) {
		this( type, template, true );
	}

	/**
	 * Constructs a SQLFunctionTemplate
	 *
	 * @param type The functions return type
	 * @param template The function template
	 * @param hasParenthesesIfNoArgs If there are no arguments, are parentheses required?
	 */
	public PatternBasedSqmFunctionTemplate(AllowableFunctionReturnType type, String template, boolean hasParenthesesIfNoArgs) {
		this.type = type;
		this.renderer = new PatternRenderer( template, hasParenthesesIfNoArgs );
		this.hasParenthesesIfNoArgs = hasParenthesesIfNoArgs;
	}

	@Override
	SelfRenderingFunctionSupport getRenderingFunctionSupport(
			List<SqmExpression> arguments,
			AllowableFunctionReturnType impliedResultType) {
		return this;
	}

	@Override
	public AllowableFunctionReturnType functionReturnType() {
		return type;
	}

	@Override
	public SqmFunctionTemplate getSqmFunctionTemplate() {
		return this;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<Expression> sqlAstArguments,
			SqlAstWalker walker,
			SessionFactoryImplementor sessionFactory) {
		sqlAppender.appendSql( renderer.render( sqlAstArguments, sessionFactory ) );
	}
}
