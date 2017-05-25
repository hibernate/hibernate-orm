/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.spi;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.dialect.function.TemplateRenderer;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.produce.internal.PatternRenderer;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.function.FunctionSqmExpression;
import org.hibernate.sql.ast.produce.spi.SqlAstFunctionProducer;
import org.hibernate.sql.ast.produce.sqm.spi.SqmToSqlAstConverter;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.type.Type;

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
public class PatternBasedSqmFunctionTemplate implements SqmFunctionTemplate, SqlAstFunctionProducer {
	private final Type type;
	private final PatternRenderer renderer;
	private final boolean hasParenthesesIfNoArgs;

	/**
	 * Constructs a SQLFunctionTemplate
	 *
	 * @param type The functions return type
	 * @param template The function template
	 */
	public PatternBasedSqmFunctionTemplate(Type type, String template) {
		this( type, template, true );
	}

	/**
	 * Constructs a SQLFunctionTemplate
	 *
	 * @param type The functions return type
	 * @param template The function template
	 * @param hasParenthesesIfNoArgs If there are no arguments, are parentheses required?
	 */
	public PatternBasedSqmFunctionTemplate(Type type, String template, boolean hasParenthesesIfNoArgs) {
		this.type = type;
		this.renderer = new PatternRenderer( template );
		this.hasParenthesesIfNoArgs = hasParenthesesIfNoArgs;
	}

	@Override
	public FunctionSqmExpression makeSqmFunctionExpression(
			List<SqmExpression> arguments,
			AllowableFunctionReturnType impliedResultType) {
		return this;
	}

	@Override
	public Expression convertToSqlAst(SqmToSqlAstConverter walker) {
		return new
		return null;
	}


	@Override
	public String render(Type argumentType, List args, SessionFactoryImplementor factory) {
		return renderer.render( args, factory );
	}

	@Override
	public Type getReturnType(Type argumentType) throws QueryException {
		return type;
	}

	@Override
	public boolean hasArguments() {
		return renderer.getAnticipatedNumberOfArguments() > 0;
	}

	@Override
	public boolean hasParenthesesIfNoArguments() {
		return hasParenthesesIfNoArgs;
	}
	
	@Override
	public String toString() {
		return renderer.getTemplate();
	}
}
