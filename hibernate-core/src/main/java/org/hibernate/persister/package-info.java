/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
 *
 * @apiNote This package is considered an SPI, meaning it is intended for use
 * by internal code and by integrations.  It is not supported for application use.
 * Be aware that its backwards compatibility guarantee is tied defined by SPI which
 * is less strict than API, which <b>is</b> intended for application use (things
 * like {@link org.hibernate.SessionFactory}, {@link org.hibernate.Session},
 * {@link org.hibernate.Transaction}, etc.).
 */
package org.hibernate.persister;
