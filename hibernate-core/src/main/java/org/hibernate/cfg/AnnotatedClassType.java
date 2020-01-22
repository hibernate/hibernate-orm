/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;


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
	 * has @Entity annotation
	 */
	ENTITY,
	/**
	 * has an @Embeddable annotation
	 */
	EMBEDDABLE,
	/**
	 * has @EmbeddedSuperclass annotation
	 */
	EMBEDDABLE_SUPERCLASS
}
