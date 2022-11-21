/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

/**
 * Enumerates the options for lazy loading of a
 * {@linkplain jakarta.persistence.ManyToOne many to one}
 * or {@linkplain jakarta.persistence.OneToOne one to one}
 * association.
 *
 * @author Emmanuel Bernard
 *
 * @see LazyToOne
 */
public enum LazyToOneOption {
	/**
	 * The association is always loaded eagerly. The identifier
	 * and concrete type of the associated entity instance are
	 * always known immediately.
	 */
	FALSE,
	/**
	 * The association is proxied and loaded lazily, by
	 * intercepting calls on the proxy object.
	 * <ul>
	 * <li>The program may obtain the entity identifier value
	 *     of an unfetched proxy, without triggering lazy
	 *     fetching.
	 * <li>For a polymorphic association, the proxy does not
	 *     have the concrete type of the proxied instance, and
	 *     so {@link org.hibernate.Hibernate#getClass(Object)}
	 *     must be used in place of {@link Object#getClass()}
	 *     and the Java {@code instanceof} operator.
	 * </ul>
	 */
	PROXY,
	/**
	 * The association is loaded lazily when the field holding
	 * the reference to the associated object is first accessed.
	 * <ul>
	 * <li>There is no way for the program to obtain the
	 *     identifier of the associated entity without triggering
	 *     lazy fetching, and therefore this option is not
	 *     recommended.
	 * <li>On the other hand, {@link Object#getClass()} and the
	 *     {@code instanceof} operator may be used even if the
	 *     association is polymorphic.
	 * </ul>
	 * Bytecode enhancement is required. If the class is not
	 * enhanced, this option is equivalent to {@link #PROXY}.
	 * <p>
	 * <strong>Currently, Hibernate does not support this setting
	 * for polymorphic associations, and instead falls back to
	 * {@link #PROXY}!</strong>
	 */
	NO_PROXY
}
