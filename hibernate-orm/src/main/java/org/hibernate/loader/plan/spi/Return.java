/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.spi;

/**
 * Represents a return value in the query results.  Not the same as a result (column) in the JDBC ResultSet!
 * <p/>
 * Return is distinctly different from a {@link org.hibernate.loader.plan.spi.Fetch} and so modeled as completely separate hierarchy.
 *
 * @see ScalarReturn
 * @see EntityReturn
 * @see CollectionReturn
 *
 * @author Steve Ebersole
 */
public interface Return {
}
