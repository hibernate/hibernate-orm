/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * A persister defines a persistence mechanism and/or mapping strategy
 * for a collection or entity:
 * <ul>
 * <li>An {@link org.hibernate.persister.entity.EntityPersister}
 *     defines a mechanism for persisting instances of a certain
 *     entity class.
 * <li>A {@link org.hibernate.persister.collection.CollectionPersister}
 *     defines a mechanism for persisting instances of a given
 *     collection role.
 * </ul>
 */
package org.hibernate.persister;
