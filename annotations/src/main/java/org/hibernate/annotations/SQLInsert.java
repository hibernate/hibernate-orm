//$Id:$
package org.hibernate.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 * SqlInsert Annotation for overwriting Hibernate default INSERT INTO method
 *
 * @author László Benke
 */
@Target( {TYPE, FIELD, METHOD} )
@Retention( RUNTIME )
public @interface SQLInsert {
	/**
	 * Procedure name or INSERT STATEMENT
	 */
	String sql();

	/**
	 * Is the statement using stored procedure or not
	 */
	boolean callable() default false;

	/**
	 * For persistence operation what style of determining results (success/failure) is to be used.
	 */
	ResultCheckStyle check() default ResultCheckStyle.NONE;
}
