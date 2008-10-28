//$Id$
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

/**
 * Define an explicit target,a voiding reflection and generics resolving
 *
 * @author Emmanuel Bernard
 */
@java.lang.annotation.Target({ElementType.FIELD, ElementType.METHOD})
@Retention( RetentionPolicy.RUNTIME )
public @interface Target {
	Class value();
}
