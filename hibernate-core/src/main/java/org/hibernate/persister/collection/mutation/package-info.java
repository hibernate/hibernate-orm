/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
