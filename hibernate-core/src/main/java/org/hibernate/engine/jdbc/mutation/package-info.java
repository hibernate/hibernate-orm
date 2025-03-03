/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * Package defining support for executing mutation SQL statements produced by an
 * {@linkplain org.hibernate.persister.entity.EntityPersister entity persister} or
 * {@linkplain org.hibernate.persister.collection.CollectionPersister collection
 * persister}.
 * <p>
 * The {@link org.hibernate.engine.jdbc.mutation.MutationExecutor} is usually
 * called by the various SQL insert, update, and delete
 * {@linkplain org.hibernate.persister.entity.mutation.AbstractMutationCoordinator
 * coordinators} defined in {@link org.hibernate.persister.entity.mutation}
 * and {@link org.hibernate.persister.collection.mutation}.
 *
 * @author Steve Ebersole
 */
@Incubating
package org.hibernate.engine.jdbc.mutation;

import org.hibernate.Incubating;
