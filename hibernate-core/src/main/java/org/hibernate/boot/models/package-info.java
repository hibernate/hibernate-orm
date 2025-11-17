/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * Overall, this module is responsible for taking
 * {@linkplain org.hibernate.boot.model.process.spi.ManagedResources managed-resources} and
 * building the {@code hibernate-models} model ({@linkplain org.hibernate.models.spi.ClassDetails}, etc.)
 * to ultimately be bound into Hibernate's {@linkplain org.hibernate.mapping boot-time model}.
 *
 * @apiNote This entire package is considered incubating
 *
 * @author Steve Ebersole
 */
@Incubating
package org.hibernate.boot.models;

import org.hibernate.Incubating;
