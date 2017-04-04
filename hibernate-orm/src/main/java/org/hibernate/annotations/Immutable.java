/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Mark an Entity, a Collection, or an Attribute type as immutable. No annotation means the element is mutable.
 * <p>
 * An immutable entity may not be updated by the application. Updates to an immutable
 * entity will be ignored, but no exception is thrown. &#064;Immutable  must be used on root entities only. 
 * </p>
 * <p>
 * &#064;Immutable placed on a collection makes the collection immutable, meaning additions and 
 * deletions to and from the collection are not allowed. A <i>HibernateException</i> is thrown in this case. 
 * </p>
 * <p>
 * An immutable attribute type will not be copied in the currently running Persistence Context in order to detect if the underlying value is dirty. As a result loading the entity will require less memory
 * and checking changes will be much faster.
 * </p>
 *
 * @author Emmanuel Bernard
 */
@java.lang.annotation.Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@Retention( RetentionPolicy.RUNTIME )
public @interface Immutable {
}
