/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * Support for {@link org.hibernate.query.sqm.mutation.spi.SqmMutationStrategy}
 * implementations that first selecting all matching ids back into memory and then using
 * those matching ids to update/delete against each table.
 */
package org.hibernate.query.sqm.mutation.spi.inline;
