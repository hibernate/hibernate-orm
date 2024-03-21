/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */

/**
 * Overall, this module is responsible for taking a
 * {@linkplain org.hibernate.boot.model.process.spi.ManagedResources managed-resources} and
 * table them into Hibernate's {@linkplain org.hibernate.mapping boot-time model}.
 * <p/>
 * Works in 2 broad phases -<ol>
 *     <li>
 *         First we {@linkplain org.hibernate.boot.models.categorize categorize} the application's domain model,
 *         as understood through {@linkplain org.hibernate.boot.model.process.spi.ManagedResources},
 *         and produce a {@linkplain org.hibernate.boot.models.categorize.spi.CategorizedDomainModel}
 *     </li>
 *     <li>
 *         Finally, the categorized metamodel is {@linkplain org.hibernate.boot.models.bind bound}
 *         into Hibernate's {@linkplain org.hibernate.mapping boot-time model}.
 *     </li>
 * </ol>
 *
 * @author Steve Ebersole
 */
package org.hibernate.boot.models;
