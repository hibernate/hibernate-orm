/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
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
 */
package org.hibernate.dialect.function;

import org.hibernate.Hibernate;
import org.hibernate.QueryException;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.type.Type;

import java.util.List;
import java.util.ArrayList;

/**
 * A {@link org.hibernate.dialect.function.SQLFunction} providing support for implementing TRIM functionality
 * (as defined by both the ANSI SQL and JPA specs) in cases where the dialect may not support the full <tt>trim</tt>
 * function itself.
 * <p/>
 * Follows the <a href="http://en.wikipedia.org/wiki/Template_method_pattern">template</a> pattern in order to implement
 * the {@link #render} method.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractAnsiTrimEmulationFunction implements SQLFunction {
	/**
	 * {@inheritDoc} 
	 */
	public final Type getReturnType(Type columnType, Mapping mapping) throws QueryException {
		return Hibernate.STRING;
	}

	/**
	 * {@inheritDoc}
	 */
	public final boolean hasArguments() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public final boolean hasParenthesesIfNoArguments() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public final String render(List args, SessionFactoryImplementor factory) throws QueryException {
		// According to both the ANSI-SQL and JPA specs, trim takes a variable number of parameters between 1 and 4.
		// at least one paramer (trimSource) is required.  From the SQL spec:
		//
		// <trim function> ::=
		//      TRIM <left paren> <trim operands> <right paren>
		//
		// <trim operands> ::=
		//      [ [ <trim specification> ] [ <trim character> ] FROM ] <trim source>
		//
		// <trim specification> ::=
		//      LEADING
		//      | TRAILING
		//      | BOTH
		//
		// If <trim specification> is omitted, BOTH is assumed.
		// If <trim character> is omitted, space is assumed
		if ( args.size() == 1 ) {
			// we have the form: trim(trimSource)
			//      so we trim leading and trailing spaces
			return resolveBothSpaceTrimFunction().render( args, factory );			// EARLY EXIT!!!!
		}
		else if ( "from".equalsIgnoreCase( ( String ) args.get( 0 ) ) ) {
			// we have the form: trim(from trimSource).
			//      This is functionally equivalent to trim(trimSource)
			return resolveBothSpaceTrimFromFunction().render( args, factory );  		// EARLY EXIT!!!!
		}
		else {
			// otherwise, a trim-specification and/or a trim-character
			// have been specified;  we need to decide which options
			// are present and "do the right thing"
			boolean leading = true;         // should leading trim-characters be trimmed?
			boolean trailing = true;        // should trailing trim-characters be trimmed?
			String trimCharacter;    		// the trim-character (what is to be trimmed off?)
			String trimSource;       		// the trim-source (from where should it be trimmed?)

			// potentialTrimCharacterArgIndex = 1 assumes that a
			// trim-specification has been specified.  we handle the
			// exception to that explicitly
			int potentialTrimCharacterArgIndex = 1;
			String firstArg = ( String ) args.get( 0 );
			if ( "leading".equalsIgnoreCase( firstArg ) ) {
				trailing = false;
			}
			else if ( "trailing".equalsIgnoreCase( firstArg ) ) {
				leading = false;
			}
			else if ( "both".equalsIgnoreCase( firstArg ) ) {
			}
			else {
				potentialTrimCharacterArgIndex = 0;
			}

			String potentialTrimCharacter = ( String ) args.get( potentialTrimCharacterArgIndex );
			if ( "from".equalsIgnoreCase( potentialTrimCharacter ) ) { 
				trimCharacter = "' '";
				trimSource = ( String ) args.get( potentialTrimCharacterArgIndex + 1 );
			}
			else if ( potentialTrimCharacterArgIndex + 1 >= args.size() ) {
				trimCharacter = "' '";
				trimSource = potentialTrimCharacter;
			}
			else {
				trimCharacter = potentialTrimCharacter;
				if ( "from".equalsIgnoreCase( ( String ) args.get( potentialTrimCharacterArgIndex + 1 ) ) ) {
					trimSource = ( String ) args.get( potentialTrimCharacterArgIndex + 2 );
				}
				else {
					trimSource = ( String ) args.get( potentialTrimCharacterArgIndex + 1 );
				}
			}

			List argsToUse = new ArrayList();
			argsToUse.add( trimSource );
			argsToUse.add( trimCharacter );

			if ( trimCharacter.equals( "' '" ) ) {
				if ( leading && trailing ) {
					return resolveBothSpaceTrimFunction().render( argsToUse, factory );
				}
				else if ( leading ) {
					return resolveLeadingSpaceTrimFunction().render( argsToUse, factory );
				}
				else {
					return resolveTrailingSpaceTrimFunction().render( argsToUse, factory );
				}
			}
			else {
				if ( leading && trailing ) {
					return resolveBothTrimFunction().render( argsToUse, factory );
				}
				else if ( leading ) {
					return resolveLeadingTrimFunction().render( argsToUse, factory );
				}
				else {
					return resolveTrailingTrimFunction().render( argsToUse, factory );
				}
			}
		}
	}

	/**
	 * Resolve the function definition which should be used to trim both leading and trailing spaces.
	 * <p/>
	 * In this form, the imput arguments is missing the <tt>FROM</tt> keyword.
	 *
	 * @return The sql function
	 */
	protected abstract SQLFunction resolveBothSpaceTrimFunction();

	/**
	 * Resolve the function definition which should be used to trim both leading and trailing spaces.
	 * <p/>
	 * The same as {#link resolveBothSpaceTrimFunction} except that here the<tt>FROM</tt> is included and
	 * will need to be accounted for during {@link SQLFunction#render} processing.
	 * 
	 * @return The sql function
	 */
	protected abstract SQLFunction resolveBothSpaceTrimFromFunction();

	/**
	 * Resolve the function definition which should be used to trim leading spaces.
	 *
	 * @return The sql function
	 */
	protected abstract SQLFunction resolveLeadingSpaceTrimFunction();

	/**
	 * Resolve the function definition which should be used to trim trailing spaces.
	 *
	 * @return The sql function
	 */
	protected abstract SQLFunction resolveTrailingSpaceTrimFunction();

	/**
	 * Resolve the function definition which should be used to trim the specified character from both the
	 * beginning (leading) and end (trailing) of the trim source.
	 *
	 * @return The sql function
	 */
	protected abstract SQLFunction resolveBothTrimFunction();

	/**
	 * Resolve the function definition which should be used to trim the specified character from the
	 * beginning (leading) of the trim source.
	 *
	 * @return The sql function
	 */
	protected abstract SQLFunction resolveLeadingTrimFunction();

	/**
	 * Resolve the function definition which should be used to trim the specified character from the
	 * end (trailing) of the trim source.
	 *
	 * @return The sql function
	 */
	protected abstract SQLFunction resolveTrailingTrimFunction();
}
