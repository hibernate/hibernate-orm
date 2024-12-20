/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */

/**
 * Overall, this module is responsible for taking
 * {@linkplain org.hibernate.boot.model.process.spi.ManagedResources managed-resources} and
 * building the {@code hibernate-models} model ({@linkplain org.hibernate.models.spi.ClassDetails}, etc.)
 * to ultimately be bound into Hibernate's {@linkplain org.hibernate.mapping boot-time model}.
 *
 * @author Steve Ebersole
 */
package org.hibernate.boot.models;
