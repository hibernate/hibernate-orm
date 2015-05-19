/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/**
 * Collectively models the concept of transaction coordination through the
 * "data store" specific notion of a transaction.  In Hibernate ORM uses this
 * correlates to the JDBC notion of a transaction, which (unfortunately) is
 * not modeled by an actual contract.  Instead JDBC models transaction control
 * via its Connection contract.  Here we use
 * {@link org.hibernate.resource.transaction.backend.jdbc.spi.JdbcResourceTransaction}
 * as the encapsulation for conceptual JDBC transaction.  It also helps isolate the
 * {@link org.hibernate.resource.transaction} and {@link org.hibernate.resource.jdbc}
 * packages from circularity.  Lastly it does somewhat allow for potentially abstracting
 * non-JDBC data stores into this transaction handling utilizing its data store specific
 * transaction mechanism.
 */
package org.hibernate.resource.transaction.backend.jdbc;
