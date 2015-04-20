/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

/**
 * Collectively models the concept of transaction coordination through the
 * "data store" specific notion of a transaction.  In Hibernate ORM uses this
 * correlates to the JDBC notion of a transaction, which (unfortunately) is
 * not modeled by an actual contract.  Instead JDBC models transaction control
 * via its Connection contract.  Here we use
 * {@link org.hibernate.resource.transaction.backend.store.spi.DataStoreTransaction}
 * as the encapsulation for conceptual JDBC transaction.  It also helps isolate the
 * {@link org.hibernate.resource.transaction} and {@link org.hibernate.resource.jdbc}
 * packages from circularity.  Lastly it does somewhat allow for potentially abstracting
 * non-JDBC data stores into this transaction handling utilizing its data store specific
 * transaction mechanism.
 */
package org.hibernate.resource.transaction.backend.store;
