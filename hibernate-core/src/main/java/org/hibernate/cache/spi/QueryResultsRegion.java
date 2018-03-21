/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi;

/**
 * Defines the contract for a cache region which will specifically be used to
 * store query results.
 *
 * @author Steve Ebersole
 */
public interface QueryResultsRegion extends DirectAccessRegion {
}
