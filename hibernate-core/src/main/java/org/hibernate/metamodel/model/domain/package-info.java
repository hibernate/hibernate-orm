/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * Defines the runtime domain metamodel, which describes the Java aspect of
 * the application's domain model parts (entities, attributes).
 * <p>
 * The API defined here extends and implements the standard
 * {@linkplain jakarta.persistence.metamodel JPA metamodel}.
 * <p>
 * This metamodel is used in {@linkplain org.hibernate.query query} handling.
 *
 * @see jakarta.persistence.metamodel
 */
@Incubating
package org.hibernate.metamodel.model.domain;

import org.hibernate.Incubating;
