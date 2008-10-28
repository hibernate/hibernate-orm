//$Id$
package org.hibernate.annotations;

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Define the lazy status of a ToOne association
 * (ie OneToOne or ManyToOne)
 *
 * @author Emmanuel Bernard
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LazyToOne {
	LazyToOneOption value();
}
