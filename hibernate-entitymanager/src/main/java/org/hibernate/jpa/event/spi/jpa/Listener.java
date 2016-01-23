/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.event.spi.jpa;

/**
 * Encapsulates access to the listener instance for listener callbacks
 * ({@link javax.persistence.EntityListeners}).
 *
 * @author Steve Ebersole
 */
public interface Listener<T> {
	T getListener();
}
