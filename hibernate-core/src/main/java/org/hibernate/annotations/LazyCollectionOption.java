/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.util.Collection;
import java.util.Map;

/**
 *  Enumerates the options for lazy loading of a
 *  {@linkplain jakarta.persistence.ElementCollection collection},
 *  {@linkplain jakarta.persistence.ManyToOne many to one association},
 *  or {@linkplain jakarta.persistence.ManyToMany many to many association}.
 *
 * @author Emmanuel Bernard
 *
 * @see LazyCollection
 *
 * @deprecated
 * <ul>
 * <li>Use the JPA-defined {@link jakarta.persistence.FetchType#EAGER}
 *     instead of {@code LazyCollection(FALSE)}.
 * <li>Use static methods of {@link org.hibernate.Hibernate},
 *     for example {@link org.hibernate.Hibernate#size(Collection)},
 *     {@link org.hibernate.Hibernate#contains(Collection, Object)}, or
 *     {@link org.hibernate.Hibernate#get(Map, Object)} instead
 *     of {@code LazyCollection(EXTRA)}.
 * </ul>
 */
@Deprecated
public enum LazyCollectionOption {
	/**
	 * The collection is always loaded eagerly, and all its
	 * elements are available immediately. However, access to
	 * the collection is still mediated by an instance of
	 * {@link org.hibernate.collection.spi.PersistentCollection},
	 * which tracks modifications to the collection.
	 *
	 * @deprecated use {@link jakarta.persistence.FetchType#EAGER}
	 */
	@Deprecated
	FALSE,
	/**
	 * The underlying Java collection is proxied by an instance of
	 * {@link org.hibernate.collection.spi.PersistentCollection}
	 * and lazily fetched when a method of the proxy is called.
	 * All elements of the collection are retrieved at once.
	 *
	 * @deprecated use {@link jakarta.persistence.FetchType#LAZY}
	 */
	@Deprecated
	TRUE,
	/**
	 * The underlying Java collection is proxied by an instance of
	 * {@link org.hibernate.collection.spi.PersistentCollection}
	 * and its state is fetched lazily from the database as needed,
	 * when methods of the proxy are called. When reasonable, the
	 * proxy will avoid fetching all elements of the collection
	 * at once.
	 *
	 * @deprecated use operations of {@link org.hibernate.Hibernate}
	 */
	@Deprecated
	EXTRA
}
