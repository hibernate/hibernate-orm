/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
