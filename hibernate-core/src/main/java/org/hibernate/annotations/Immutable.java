/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static java.lang.annotation.ElementType.*;

/**
 * Marks an entity, collection, or attribute of an entity as immutable. The absence of this
 * annotation means the element is mutable.
 *
 * <h3>Immutable entities</h3>
 * <p>
 * Changes made in memory to the state of an immutable entity are never synchronized to
 * the database. The changes are ignored, with no exception thrown.
 * <p>
 * An immutable entity need not be dirty-checked, and so Hibernate does not need to
 * maintain a snapshot of its state. This may help reduce memory allocation.
 * Note that it's also possible to obtain an entity in read-only mode in a given session,
 * and this has similar benefits.
 * <p>
 * In a mapped inheritance hierarchy, {@code @Immutable} may be applied only to the root
 * entity, and is inherited by entity subclasses. To make just one entity in the hierarchy
 * immutable, annotate its attributes individually.
 *
 * <h3>Immutable basic-valued attributes</h3>
 * <p>
 * A mutable entity may have an immutable field or property.
 * <p>
 * An immutable attribute is ignored by the dirty-checking process, and so the persistence
 * context does not need to keep track of its state. This may help reduce memory allocation.
 *
 * <h3>Immutable collections</h3>
 * <p>
 * An immutable collection may not be modified.
 * <p>
 * A {@link org.hibernate.HibernateException} is thrown if an element is added to or
 * removed from the collection.
 *
 * <h3>Immutable for converters</h3>
 * <p>
 * {@code @Immutable} may also be used to mark a Java type handled by a JPA
 * {@link jakarta.persistence.AttributeConverter} as immutable, circumventing the need to treat
 * it as mutable.
 * <p>
 * Either:
 * <ul>
 * <li>annotate the Java type itself, or
 * <li>annotate the {@code AttributeConverter} class.
 * </ul>
 * <p>
 * This is not the same as marking the attribute {@code @Immutable}. A mutable attribute may
 * have a type whose values are immutable.
 *
 * @author Emmanuel Bernard
 *
 * @see org.hibernate.Session#setDefaultReadOnly(boolean)
 * @see org.hibernate.Session#setReadOnly(Object, boolean)
 * @see org.hibernate.query.Query#setReadOnly(boolean)
 */
@Target({TYPE, METHOD, FIELD})
@Retention( RetentionPolicy.RUNTIME )
public @interface Immutable {
}
