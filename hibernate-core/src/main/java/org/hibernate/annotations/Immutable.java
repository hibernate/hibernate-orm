/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static java.lang.annotation.ElementType.*;

/**
 * Marks an entity, collection, or attribute as immutable. The absence of this annotation
 * means the element is mutable.
 * <ul>
 * <li>
 *     Changes made in memory to the state of an immutable entity are never synchronized to
 *     the database. The changes are ignored, with no exception thrown. In a mapped inheritance
 *     hierarchy, {@code @Immutable} may be applied only to the root entity.
 * </li>
 * <li>
 *     An immutable collection may not be modified. A {@link org.hibernate.HibernateException}
 *     is thrown if an element is added to or removed from the collection.
 * </li>
 * <li>
 *     An immutable attribute is ignored by the dirty-checking process, and so the persistence
 *     context does not need to keep track of its state. This may help reduce memory allocation.
 * </li>
 * </ul>
 *
 * @author Emmanuel Bernard
 */
@Target({TYPE, METHOD, FIELD})
@Retention( RetentionPolicy.RUNTIME )
public @interface Immutable {
}
