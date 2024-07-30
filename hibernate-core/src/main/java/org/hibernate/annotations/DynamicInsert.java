/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies that SQL {@code insert} statements for the annotated entity
 * are generated dynamically, and only include the columns to which a
 * non-null value must be assigned.
 * <p>
 * This might result in improved performance if an entity is likely to
 * have many null attributes when it is first made persistent. However,
 * there is a cost associated with generating the SQL at runtime.
 *
 * @author Steve Ebersole
 */
@Target( TYPE )
@Retention( RUNTIME )
public @interface DynamicInsert {
}
