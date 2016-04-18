/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.event.spi.jpa;

/**
 * Contract for building instances of JPA callback listener classes.
 * <p/>
 * Listener instances should be uniqued by Class such that the same instance is
 * returned for any calls to {@link #buildListener} with the same class.
 *
 * @see javax.persistence.EntityListeners
 *
 * @author Steve Ebersole
 */
public interface ListenerFactory {
	<T> Listener<T> buildListener(Class<T>  listenerClass);
	void release();
}
