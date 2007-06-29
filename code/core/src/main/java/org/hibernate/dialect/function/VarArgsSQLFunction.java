//$Id: VarArgsSQLFunction.java 6608 2005-04-29 15:32:30Z oneovthafew $
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.type.Type;

/**
 * Support for slightly more general templating than <tt>StandardSQLFunction</tt>,
 * with an unlimited number of arguments.
 * @author Gavin King
 */
public class VarArgsSQLFunction implements SQLFunction {

	private final String begin;
	private final String sep;
	private final String end;
	private final Type type;
	
	public VarArgsSQLFunction(Type type, String begin, String sep, String end) {
		this.begin = begin;
		this.sep = sep;
		this.end = end;
		this.type = type;
	}

	public VarArgsSQLFunction(String begin, String sep, String end) {
		this.begin = begin;
		this.sep = sep;
		this.end = end;
		this.type = null;
	}

	public Type getReturnType(Type columnType, Mapping mapping) throws QueryException {
		return type==null ? columnType : type;
	}

	public boolean hasArguments() {
		return true;
	}

	public boolean hasParenthesesIfNoArguments() {
		return true;
	}

	public String render(List args, SessionFactoryImplementor factory) throws QueryException {
		StringBuffer buf = new StringBuffer().append(begin);
		for ( int i=0; i<args.size(); i++ ) {
			buf.append( args.get(i) );
			if (i<args.size()-1) buf.append(sep);
		}
		return buf.append(end).toString();
	}

}
