//$Id$
package org.hibernate.annotations;

import java.lang.annotation.*;

/**
 * Mark an Entity or a Collection as immutable. No annotation means the element is mutable.
 * <p>
 * An immutable entity may not be updated by the application. Updates to an immutable
 * entity will be ignored, but no exception is thrown. &#064;Immutable  must be used on root entities only. 
 * </p>
 * <p>
 * &#064;Immutable placed on a collection makes the collection immutable, meaning additions and 
 * deletions to and from the collection are not allowed. A <i>HibernateException</i> is thrown in this case. 
 * </p>
 *
 * @author Emmanuel Bernard
 */
@java.lang.annotation.Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@Retention( RetentionPolicy.RUNTIME )
public @interface Immutable {
}
