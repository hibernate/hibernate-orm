/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.internal;


import org.hibernate.annotations.Imported;

/**
 * Type of annotation of a class will give its type
 *
 * @author Emmanuel Bernard
 */
public enum AnnotatedClassType {
	/**
	 * has no relevant top level annotation
	 */
	NONE,
	/**
	 * has an {@link Imported} annotation
	 */
	IMPORTED,
	/**
	 * has an {@link jakarta.persistence.Entity} annotation
	 */
	ENTITY,
	/**
	 * has an {@link jakarta.persistence.Embeddable} annotation
	 */
	EMBEDDABLE,
	/**
	 * has {@link jakarta.persistence.MappedSuperclass} annotation
	 */
	MAPPED_SUPERCLASS
}
