/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */

/**
 * Defines support for performing mutation operations against collections.
 *
 * @apiNote The names used here are logical. E.g. "inserting a row" may actually
 *          execute an UPDATE statement instead of an INSERT. This is generally
 *          delineated based on whether there is a collection table involved or
 *          not. In terms of our usual model, this breaks down to the distinction
 *          between {@link org.hibernate.persister.collection.BasicCollectionPersister}
 *          and {@link org.hibernate.persister.collection.OneToManyPersister}.
 */
@Incubating
package org.hibernate.persister.collection.mutation;

import org.hibernate.Incubating;
