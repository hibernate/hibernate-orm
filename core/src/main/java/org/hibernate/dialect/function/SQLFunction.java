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
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.type.Type;

/**
 * Provides support routines for the HQL functions as used
 * in the various SQL Dialects
 *
 * Provides an interface for supporting various HQL functions that are
 * translated to SQL. The Dialect and its sub-classes use this interface to
 * provide details required for processing of the function.
 *
 * @author David Channon
 */
public interface SQLFunction {
	/**
	 * The return type of the function.  May be either a concrete type which
	 * is preset, or variable depending upon the type of the first function
	 * argument.
	 *
	 * @param columnType the type of the first argument
	 * @param mapping The mapping source.
	 * @return The type to be expected as a return.
	 * @throws org.hibernate.QueryException Indicates an issue resolving the return type.
	 */
	public Type getReturnType(Type columnType, Mapping mapping) throws QueryException;

	/**
	 * Does this function have any arguments?
	 *
	 * @return True if the function expects to have parameters; false otherwise.
	 */
	public boolean hasArguments();

	/**
	 * If there are no arguments, are parens required?
	 *
	 * @return True if a no-arg call of this function requires parentheses.
	 */
	public boolean hasParenthesesIfNoArguments();

	/**
	 * Render the function call as SQL fragment.
	 *
	 * @param args The function arguments
	 * @param factory The SessionFactory
	 * @return The rendered function call
	 * @throws org.hibernate.QueryException Indicates a problem rendering the
	 * function call.
	 */
	public String render(List args, SessionFactoryImplementor factory) throws QueryException;
}
