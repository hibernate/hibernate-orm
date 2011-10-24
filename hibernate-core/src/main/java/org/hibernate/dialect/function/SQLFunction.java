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
 * Provides support routines for the HQL functions as used
 * in the various SQL Dialects
 *
 * Provides an interface for supporting various HQL functions that are
 * translated to SQL. The Dialect and its sub-classes use this interface to
 * provide details required for processing of the function.
 *
 * @author David Channon
 * @author Steve Ebersole
 */
public interface SQLFunction {
	/**
	 * Does this function have any arguments?
	 *
	 * @return True if the function expects to have parameters; false otherwise.
	 */
	public boolean hasArguments();

	/**
	 * If there are no arguments, are parentheses required?
	 *
	 * @return True if a no-arg call of this function requires parentheses.
	 */
	public boolean hasParenthesesIfNoArguments();

	/**
	 * The return type of the function.  May be either a concrete type which is preset, or variable depending upon
	 * the type of the first function argument.
	 * <p/>
	 * Note, the 'firstArgumentType' parameter should match the one passed into {@link #render}
	 *
	 * @param firstArgumentType The type of the first argument
	 * @param mapping The mapping source.
	 *
	 * @return The type to be expected as a return.
	 *
	 * @throws org.hibernate.QueryException Indicates an issue resolving the return type.
	 */
	public Type getReturnType(Type firstArgumentType, Mapping mapping) throws QueryException;


	/**
	 * Render the function call as SQL fragment.
	 * <p/>
	 * Note, the 'firstArgumentType' parameter should match the one passed into {@link #getReturnType}
	 *
	 * @param firstArgumentType The type of the first argument
	 * @param arguments The function arguments
	 * @param factory The SessionFactory
	 *
	 * @return The rendered function call
	 *
	 * @throws org.hibernate.QueryException Indicates a problem rendering the
	 * function call.
	 */
	public String render(Type firstArgumentType, List arguments, SessionFactoryImplementor factory) throws QueryException;
}
