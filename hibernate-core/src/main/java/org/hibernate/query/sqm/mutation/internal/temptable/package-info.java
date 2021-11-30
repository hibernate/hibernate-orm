/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * Support for multi-table SQM mutation (insert, update, delete) operations using
 * a table to temporarily hold the matching ids.  There are 3 forms:
 *
 * 		* {@link org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableMutationStrategy} uses
 * 			local temp tables as defined by the SQL spec
 * 		* {@link org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableMutationStrategy} uses
 * 			global temp tables as defined by the SQL spec
 * 		* {@link org.hibernate.query.sqm.mutation.internal.temptable.PersistentTableMutationStrategy} uses normal table
 * 			managed by Hibernate.
 *
 * @asciidoc
 */
package org.hibernate.query.sqm.mutation.internal.temptable;
