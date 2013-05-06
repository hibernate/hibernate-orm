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
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
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
public class SQLFunctionTemplate implements SQLFunction {
	private final Type type;
	private final TemplateRenderer renderer;
	private final boolean hasParenthesesIfNoArgs;

	/**
	 * Constructs a SQLFunctionTemplate
	 *
	 * @param type The functions return type
	 * @param template The function template
	 */
	public SQLFunctionTemplate(Type type, String template) {
		this( type, template, true );
	}

	/**
	 * Constructs a SQLFunctionTemplate
	 *
	 * @param type The functions return type
	 * @param template The function template
	 * @param hasParenthesesIfNoArgs If there are no arguments, are parentheses required?
	 */
	public SQLFunctionTemplate(Type type, String template, boolean hasParenthesesIfNoArgs) {
		this.type = type;
		this.renderer = new TemplateRenderer( template );
		this.hasParenthesesIfNoArgs = hasParenthesesIfNoArgs;
	}

	@Override
	public String render(Type argumentType, List args, SessionFactoryImplementor factory) {
		return renderer.render( args, factory );
	}

	@Override
	public Type getReturnType(Type argumentType, Mapping mapping) throws QueryException {
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
