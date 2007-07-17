package org.hibernate.dialect.function;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.dialect.Dialect;

public class SQLFunctionRegistry {

	private final Dialect dialect;
	private final Map userFunctions;
	
	public SQLFunctionRegistry(Dialect dialect, Map userFunctions) {
		this.dialect = dialect;
		this.userFunctions = new HashMap();
		this.userFunctions.putAll( userFunctions );
	}
	
	public SQLFunction findSQLFunction(String functionName) {
		String name = functionName.toLowerCase();
		SQLFunction userFunction = (SQLFunction) userFunctions.get( name );
		
		return userFunction!=null?userFunction:(SQLFunction) dialect.getFunctions().get(name); // TODO: lowercasing done here. Was done "at random" before; maybe not needed at all ?
	}

	public boolean hasFunction(String functionName) {
		String name = functionName.toLowerCase();
		boolean hasUserFunction = userFunctions.containsKey ( name );
		
		return hasUserFunction || dialect.getFunctions().containsKey ( name ); // TODO: toLowerCase was not done before. Only used in Template.
	}

}
