/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/**
 * Package contains an implementation of MultiTableBulkIdStrategy based on the use
 * of a persistent (ANSI SQL term) table to hold id values.  It also holds a "session identifier"
 * column which Hibernate manages in order to isolate rows from different sessions.
 */
package org.hibernate.hql.spi.id.persistent;
