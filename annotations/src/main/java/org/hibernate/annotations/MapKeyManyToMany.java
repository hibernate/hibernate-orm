//$Id$
package org.hibernate.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.persistence.JoinColumn;

/**
 * Define the map key columns as an explicit column holding the map key
 * This is completly different from {@link javax.persistence.MapKey} which use an existing column
 * This annotation and {@link javax.persistence.MapKey} are mutually exclusive
 *
 * @author Emmanuel Bernard
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MapKeyManyToMany {
	JoinColumn[] joinColumns() default {};
	/**
	 * Represent the key class in a Map
	 * Only useful if the collection does not use generics
	 */
	Class targetEntity() default void.class;
}
