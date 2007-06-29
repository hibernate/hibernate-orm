//$Id: CastFunction.java 7368 2005-07-04 02:54:27Z oneovthafew $
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.Hibernate;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.type.Type;

/**
 * A Cach&eacute; defintion of a convert function.
 *
 * @author Jonathan Levinson
 */
public class ConvertFunction implements SQLFunction {

	public Type getReturnType(Type columnType, Mapping mapping) throws QueryException {
		return Hibernate.STRING;
	}

	public boolean hasArguments() {
		return true;
	}

	public boolean hasParenthesesIfNoArguments() {
		return true;
	}

	public String render(List args, SessionFactoryImplementor factory) throws QueryException {
		if ( args.size() != 2 && args.size() != 3 ) {
			throw new QueryException( "convert() requires two or three arguments" );
		}
		String type = ( String ) args.get( 1 );

		if ( args.size() == 2 ) {
			return "{fn convert(" + args.get( 0 ) + " , " + type + ")}";
		}
		else {
			return "convert(" + args.get( 0 ) + " , " + type + "," + args.get( 2 ) + ")";
		}
	}

}
