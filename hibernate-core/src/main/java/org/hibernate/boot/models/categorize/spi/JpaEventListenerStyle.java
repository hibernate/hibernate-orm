/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.models.categorize.spi;

/**
 * JPA defines 2 ways events callbacks can happen...
 *
 * @author Steve Ebersole
 */
public enum JpaEventListenerStyle {
	/**
	 * The event method is declared on the entity class.
	 * The annotated method should define no arguments and have a void return type.
	 */
	CALLBACK,

	/**
	 * The event method is declared on a separate "listener" class named by {@linkplain jakarta.persistence.EntityListeners}.
	 * The annotated method should accept a single argument - the entity instance - and have a void return type.
	 */
	LISTENER
}
