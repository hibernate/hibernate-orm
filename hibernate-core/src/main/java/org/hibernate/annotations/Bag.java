/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;

import org.hibernate.cfg.AvailableSettings;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Can be used to map a {@link java.util.List}-valued attribute using
 * {@link org.hibernate.metamodel.CollectionClassification#BAG} semantics.
 * <p/>
 * @apiNote Ignored if either {@link jakarta.persistence.OrderColumn} or
 * {@link ListIndexBase} is used.
 *
 * @implSpec May also be specified SessionFactory-wide using {@value AvailableSettings#DEFAULT_LIST_SEMANTICS}
 *
 * @author Steve Ebersole
 */
@java.lang.annotation.Target({METHOD, FIELD, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface Bag {
}
