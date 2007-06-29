//$Id: NvlFunction.java 6608 2005-04-29 15:32:30Z oneovthafew $
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.type.Type;

/**
 * Emulation of <tt>coalesce()</tt> on Oracle, using multiple
 * <tt>nvl()</tt> calls
 * @author Gavin King
 */
public class NvlFunction implements SQLFunction {

	public Type getReturnType(Type columnType, Mapping mapping) throws QueryException {
		return columnType;
	}

	public boolean hasArguments() {
		return true;
	}

	public boolean hasParenthesesIfNoArguments() {
		return true;
	}

	public String render(List args, SessionFactoryImplementor factory) throws QueryException {
		int lastIndex = args.size()-1;
		Object last = args.remove(lastIndex);
		if ( lastIndex==0 ) return last.toString();
		Object secondLast = args.get(lastIndex-1);
		String nvl = "nvl(" + secondLast + ", " + last + ")";
		args.set(lastIndex-1, nvl);
		return render(args, factory);
	}

	

}
