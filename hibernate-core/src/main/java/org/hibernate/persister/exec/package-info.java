/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * Package containing loaders and executors for handling:<ul>
 *     <li>JdbcSelect - execute a SELECT query and process results</li>
 *     <li>JdbcOperation - execute INSERT, UPDATE and DELETE queries</li>
 * </ul>
 *
 * @todo (6.0) Add a specific JdbcOperation subtype for DML operations.  One per DML type?
 */
package org.hibernate.persister.exec;
