//$Id$
package org.hibernate.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import javax.persistence.FetchType;
import static javax.persistence.FetchType.LAZY;

/**
 * Annotation used to mark a collection as a collection of elements or
 * a collection of embedded objects
 *
 * @author Emmanuel Bernard
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface CollectionOfElements {
	/**
	 * Represent the element class in the collection
	 * Only useful if the collection does not use generics
	 */
	Class targetElement() default void.class;

	FetchType fetch() default LAZY;
}
