//$Id$
package org.hibernate.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Whether or not update entity's version on property's change
 * If the annotation is not present, the property is involved in the optimistic lock srategy (default)
 *
 * @author Logi Ragnarsson
 */
@Target( {ElementType.METHOD, ElementType.FIELD} )
@Retention( RetentionPolicy.RUNTIME )
public @interface OptimisticLock {

	/**
	 * If true, the annotated property change will not trigger a version upgrade
	 */
	boolean excluded();

}
