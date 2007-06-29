/**
 * 
 */
package org.hibernate.dialect.function;


/**
 * Classic SUM sqlfunction that return types as it was done in Hibernate 3.1 
 * 
 * @author Max Rydahl Andersen
 *
 */
public class ClassicSumFunction extends StandardSQLFunction {
	public ClassicSumFunction() {
		super( "sum" );
	}
}