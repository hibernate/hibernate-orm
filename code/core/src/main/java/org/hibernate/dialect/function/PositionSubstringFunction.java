//$Id: PositionSubstringFunction.java 6608 2005-04-29 15:32:30Z oneovthafew $
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.QueryException;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.type.Type;

/**
 * Emulation of <tt>locate()</tt> on PostgreSQL
 * @author Gavin King
 */
public class PositionSubstringFunction implements SQLFunction {

	public Type getReturnType(Type columnType, Mapping mapping) throws QueryException {
		return Hibernate.INTEGER;
	}

	public boolean hasArguments() {
		return true;
	}

	public boolean hasParenthesesIfNoArguments() {
		return true;
	}

	public String render(List args, SessionFactoryImplementor factory) throws QueryException {
		boolean threeArgs = args.size() > 2;
		Object pattern = args.get(0);
		Object string = args.get(1);
		Object start = threeArgs ? args.get(2) : null;

		StringBuffer buf = new StringBuffer();
		if (threeArgs) buf.append('(');
		buf.append("position(").append( pattern ).append(" in ");
		if (threeArgs) buf.append( "substring(");
		buf.append( string );
		if (threeArgs) buf.append( ", " ).append( start ).append(')');
		buf.append(')');
		if (threeArgs) buf.append('+').append( start ).append("-1)");
		return buf.toString();
	}


}
