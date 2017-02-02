/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.spi;

/**
 * Marker interface for identifying {@link org.hibernate.Cache} implementations which are aware of JTA transactions
 *
 * @author Steve Ebersole
 */
public interface TransactionAwareCache {
}
