//$Id$
package org.hibernate.annotations;

import java.lang.annotation.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

/**
 * Define a tuplizer for an entity or a component
 * @author Emmanuel Bernard
 */
@java.lang.annotation.Target( {TYPE, FIELD, METHOD} )
@Retention( RUNTIME )
public @interface Tuplizer {
	/** tuplizer implementation */
	Class impl();
	/** either pojo, dynamic-map or dom4j÷ */
	String entityMode() default "pojo";
}
