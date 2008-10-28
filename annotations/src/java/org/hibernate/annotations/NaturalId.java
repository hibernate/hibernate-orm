package org.hibernate.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;


/**
 * This specifies that a property is part of the natural id of the entity.
 *
 * @author Nicol‡s Lichtmaier
 */
@Target( { METHOD, FIELD } )
@Retention( RUNTIME )
public @interface NaturalId {
	/**
	 * If this natural id component is mutable or not.
	 */
	boolean mutable() default false;
}
