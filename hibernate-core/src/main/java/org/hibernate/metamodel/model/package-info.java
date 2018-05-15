/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * Defines the parts of Hibernate's run-time view of the application model,
 * both the domain aspect as well as the relational view.
 *
 * Also see {@link org.hibernate.boot.model} for it's boot-time model, which
 * is used to incrementally collect all the "metadata source" (XML, annotations, etc).
 * This run-time model is built from the boot-time model; see
 * {@link org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationProcess}
 *
 * @apiNote This package and its sub-packages should be considered incubating
 * (see {@link org.hibernate.Incubating}).
 */
@Incubating
package org.hibernate.metamodel.model;

import org.hibernate.Incubating;
