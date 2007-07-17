/**
 * 
 */
package org.hibernate.dialect.function;

import org.hibernate.Hibernate;
import org.hibernate.engine.Mapping;
import org.hibernate.type.Type;


/**
 * Classic COUNT sqlfunction that return types as it was done in Hibernate 3.1 
 * 
 * @author Max Rydahl Andersen
 *
 */
public class ClassicCountFunction extends StandardSQLFunction {
	public ClassicCountFunction() {
		super( "count" );
	}

	public Type getReturnType(Type columnType, Mapping mapping) {
		return Hibernate.INTEGER;
	}
}