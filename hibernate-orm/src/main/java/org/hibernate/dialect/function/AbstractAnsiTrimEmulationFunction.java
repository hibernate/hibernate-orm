/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.function;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

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
	@Override
	public final boolean hasArguments() {
		return true;
	}

	@Override
	public final boolean hasParenthesesIfNoArguments() {
		return false;
	}

	@Override
	public final Type getReturnType(Type argumentType, Mapping mapping) throws QueryException {
		return StandardBasicTypes.STRING;
	}

	@Override
	public final String render(Type argumentType, List args, SessionFactoryImplementor factory) throws QueryException {
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
			return resolveBothSpaceTrimFunction().render( argumentType, args, factory );
		}
		else if ( "from".equalsIgnoreCase( (String) args.get( 0 ) ) ) {
			// we have the form: trim(from trimSource).
			//      This is functionally equivalent to trim(trimSource)
			return resolveBothSpaceTrimFromFunction().render( argumentType, args, factory );
		}
		else {
			// otherwise, a trim-specification and/or a trim-character
			// have been specified;  we need to decide which options
			// are present and "do the right thing"

			// should leading trim-characters be trimmed?
			boolean leading = true;
			// should trailing trim-characters be trimmed?
			boolean trailing = true;
			// the trim-character (what is to be trimmed off?)
			String trimCharacter;
			// the trim-source (from where should it be trimmed?)
			String trimSource;

			// potentialTrimCharacterArgIndex = 1 assumes that a
			// trim-specification has been specified.  we handle the
			// exception to that explicitly
			int potentialTrimCharacterArgIndex = 1;
			final String firstArg = (String) args.get( 0 );
			if ( "leading".equalsIgnoreCase( firstArg ) ) {
				trailing = false;
			}
			else if ( "trailing".equalsIgnoreCase( firstArg ) ) {
				leading = false;
			}
			else if ( "both".equalsIgnoreCase( firstArg ) ) {
				// nothing to do here
			}
			else {
				potentialTrimCharacterArgIndex = 0;
			}

			final String potentialTrimCharacter = (String) args.get( potentialTrimCharacterArgIndex );
			if ( "from".equalsIgnoreCase( potentialTrimCharacter ) ) { 
				trimCharacter = "' '";
				trimSource = (String) args.get( potentialTrimCharacterArgIndex + 1 );
			}
			else if ( potentialTrimCharacterArgIndex + 1 >= args.size() ) {
				trimCharacter = "' '";
				trimSource = potentialTrimCharacter;
			}
			else {
				trimCharacter = potentialTrimCharacter;
				if ( "from".equalsIgnoreCase( (String) args.get( potentialTrimCharacterArgIndex + 1 ) ) ) {
					trimSource = (String) args.get( potentialTrimCharacterArgIndex + 2 );
				}
				else {
					trimSource = (String) args.get( potentialTrimCharacterArgIndex + 1 );
				}
			}

			final List<String> argsToUse = new ArrayList<String>();
			argsToUse.add( trimSource );
			argsToUse.add( trimCharacter );

			if ( trimCharacter.equals( "' '" ) ) {
				if ( leading && trailing ) {
					return resolveBothSpaceTrimFunction().render( argumentType, argsToUse, factory );
				}
				else if ( leading ) {
					return resolveLeadingSpaceTrimFunction().render( argumentType, argsToUse, factory );
				}
				else {
					return resolveTrailingSpaceTrimFunction().render( argumentType, argsToUse, factory );
				}
			}
			else {
				if ( leading && trailing ) {
					return resolveBothTrimFunction().render( argumentType, argsToUse, factory );
				}
				else if ( leading ) {
					return resolveLeadingTrimFunction().render( argumentType, argsToUse, factory );
				}
				else {
					return resolveTrailingTrimFunction().render( argumentType, argsToUse, factory );
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
