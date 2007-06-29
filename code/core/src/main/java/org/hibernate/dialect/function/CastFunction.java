//$Id: CastFunction.java 7368 2005-07-04 02:54:27Z oneovthafew $
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.type.Type;
import org.hibernate.type.TypeFactory;

/**
 * ANSI-SQL style <tt>cast(foo as type)</tt> where the type is
 * a Hibernate type
 * @author Gavin King
 */
public class CastFunction implements SQLFunction {

	public Type getReturnType(Type columnType, Mapping mapping) throws QueryException {
		return columnType; //note there is a wierd implementation in the client side
	}

	public boolean hasArguments() {
		return true;
	}

	public boolean hasParenthesesIfNoArguments() {
		return true;
	}

	public String render(List args, SessionFactoryImplementor factory) throws QueryException {
		if ( args.size()!=2 ) {
			throw new QueryException("cast() requires two arguments");
		}
		String type = (String) args.get(1);
		int[] sqlTypeCodes = TypeFactory.heuristicType(type).sqlTypes(factory);
		if ( sqlTypeCodes.length!=1 ) {
			throw new QueryException("invalid Hibernate type for cast()");
		}
		String sqlType = factory.getDialect().getCastTypeName( sqlTypeCodes[0] );
		if (sqlType==null) {
			//TODO: never reached, since getTypeName() actually throws an exception!
			sqlType = type;
		}
		/*else {
			//trim off the length/precision/scale
			int loc = sqlType.indexOf('(');
			if (loc>-1) {
				sqlType = sqlType.substring(0, loc);
			}
		}*/
		return "cast(" + args.get(0) + " as " + sqlType + ')';
	}

}
