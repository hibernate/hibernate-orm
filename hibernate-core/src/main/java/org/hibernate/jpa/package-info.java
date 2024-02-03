/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * The packages in this namespace are responsible for implementing certain
 * requirements of the JPA specification, especially things which are only
 * needed when Hibernate is acting as a JPA <em>persistence provider</em>.
 * <p>
 * This package contains an
 * {@linkplain org.hibernate.jpa.HibernatePersistenceProvider implementation}
 * of a JPA {@link jakarta.persistence.spi.PersistenceProvider}. You may
 * choose Hibernate as your JPA persistence provider by including the
 * following line in {@code persistence.xml}:
 * <pre>{@code <provider>org.hibernate.jpa.HibernatePersistenceProvider</provider>}</pre>
 * When working with the Hibernate persistence provider, keep in mind that:
 * <ul>
 * <li>the {@link jakarta.persistence.EntityManagerFactory} is also a
 *     {@link org.hibernate.SessionFactory},
 * <li>every {@link jakarta.persistence.EntityManager} is also a
 *     {@link org.hibernate.Session}, and
 * <li>every {@link jakarta.persistence.Query} is also a
 *     {@link org.hibernate.query.Query}.
 * </ul>
 * <p>
 * Thus, Hibernate's many powerful extensions to the JPA specification are
 * always readily accessible.
 * <ul>
 * <li>The subpackage {@link org.hibernate.jpa.boot.spi} contains the SPI of
 *     this persistence provider, including an SPI used to
 *     {@linkplain org.hibernate.jpa.boot.spi.Bootstrap bootstrap} the JPA
 *     provider, and interfaces which may be implemented to contribute extensions
 *     during of the bootstrap process.
 * <li>The package {@link org.hibernate.jpa.event.spi org.hibernate.jpa.event}
 *     implements support for JPA {@linkplain jakarta.persistence.EntityListeners
 *     event listeners}.
 * <li>The package {@link org.hibernate.jpa.spi} provides SPIs for managing cases
 *     where Hibernate intentionally violates the JPA specification by default
 *     (something Hibernate only does when it has a really good reason to do so).
 * </ul>
 * <p>
 * Finally, we have two interfaces which enumerate the JPA
 * {@linkplain jakarta.persistence.Query#setHint query hints} recognized by
 * Hibernate:
 * <ul>
 * <li>{@link org.hibernate.jpa.SpecHints} enumerates the standard hints
 *     defined by the JPA specification, and
 * <li>{@link org.hibernate.jpa.HibernateHints} enumerates hints defined
 *     by Hibernate.
 * </ul>
 */
package org.hibernate.jpa;
