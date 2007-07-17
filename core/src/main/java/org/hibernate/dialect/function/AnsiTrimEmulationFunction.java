package org.hibernate.dialect.function;

import org.hibernate.Hibernate;
import org.hibernate.QueryException;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.type.Type;

import java.util.List;
import java.util.ArrayList;

/**
 * A {@link SQLFunction} implementation that emulates the ANSI SQL trim function
 * on dialects which do not support the full definition.  However, this function
 * definition does assume the availability of ltrim, rtrim, and replace functions
 * which it uses in various combinations to emulate the desired ANSI trim()
 * functionality.
 *
 * @author Steve Ebersole
 */
public class AnsiTrimEmulationFunction implements SQLFunction {

	private static final SQLFunction LEADING_SPACE_TRIM = new SQLFunctionTemplate( Hibernate.STRING, "ltrim( ?1 )");
	private static final SQLFunction TRAILING_SPACE_TRIM = new SQLFunctionTemplate( Hibernate.STRING, "rtrim( ?1 )");
	private static final SQLFunction BOTH_SPACE_TRIM = new SQLFunctionTemplate( Hibernate.STRING, "ltrim( rtrim( ?1 ) )");
	private static final SQLFunction BOTH_SPACE_TRIM_FROM = new SQLFunctionTemplate( Hibernate.STRING, "ltrim( rtrim( ?2 ) )");

	private static final SQLFunction LEADING_TRIM = new SQLFunctionTemplate( Hibernate.STRING, "replace( replace( rtrim( replace( replace( ?1, ' ', '${space}$' ), ?2, ' ' ) ), ' ', ?2 ), '${space}$', ' ' )" );
	private static final SQLFunction TRAILING_TRIM = new SQLFunctionTemplate( Hibernate.STRING, "replace( replace( ltrim( replace( replace( ?1, ' ', '${space}$' ), ?2, ' ' ) ), ' ', ?2 ), '${space}$', ' ' )" );
	private static final SQLFunction BOTH_TRIM = new SQLFunctionTemplate( Hibernate.STRING, "replace( replace( ltrim( rtrim( replace( replace( ?1, ' ', '${space}$' ), ?2, ' ' ) ) ), ' ', ?2 ), '${space}$', ' ' )" );

	public Type getReturnType(Type columnType, Mapping mapping) throws QueryException {
		return Hibernate.STRING;
	}

	public boolean hasArguments() {
		return true;
	}

	public boolean hasParenthesesIfNoArguments() {
		return false;
	}

	public String render(List args, SessionFactoryImplementor factory) throws QueryException {
		// according to both the ANSI-SQL and EJB3 specs, trim can either take
		// exactly one parameter or a variable number of parameters between 1 and 4.
		// from the SQL spec:
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
		// If only <trim specification> is omitted, BOTH is assumed;
		// if <trim character> is omitted, space is assumed
		if ( args.size() == 1 ) {
			// we have the form: trim(trimSource)
			//      so we trim leading and trailing spaces
			return BOTH_SPACE_TRIM.render( args, factory );
		}
		else if ( "from".equalsIgnoreCase( ( String ) args.get( 0 ) ) ) {
			// we have the form: trim(from trimSource).
			//      This is functionally equivalent to trim(trimSource)
			return BOTH_SPACE_TRIM_FROM.render( args, factory );
		}
		else {
			// otherwise, a trim-specification and/or a trim-character
			// have been specified;  we need to decide which options
			// are present and "do the right thing"
			boolean leading = true;         // should leading trim-characters be trimmed?
			boolean trailing = true;        // should trailing trim-characters be trimmed?
			String trimCharacter = null;    // the trim-character
			String trimSource = null;       // the trim-source

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

			List argsToUse = null;
			argsToUse = new ArrayList();
			argsToUse.add( trimSource );
			argsToUse.add( trimCharacter );

			if ( trimCharacter.equals( "' '" ) ) {
				if ( leading && trailing ) {
					return BOTH_SPACE_TRIM.render( argsToUse, factory );
				}
				else if ( leading ) {
					return LEADING_SPACE_TRIM.render( argsToUse, factory );
				}
				else {
					return TRAILING_SPACE_TRIM.render( argsToUse, factory );
				}
			}
			else {
				if ( leading && trailing ) {
					return BOTH_TRIM.render( argsToUse, factory );
				}
				else if ( leading ) {
					return LEADING_TRIM.render( argsToUse, factory );
				}
				else {
					return TRAILING_TRIM.render( argsToUse, factory );
				}
			}
		}
	}
}
