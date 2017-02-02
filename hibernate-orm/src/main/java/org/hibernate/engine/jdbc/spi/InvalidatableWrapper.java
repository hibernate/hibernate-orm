/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.spi;


/**
 * Specialized {@link JdbcWrapper} contract for wrapped objects that can additioanlly be invalidated
 *
 * @author Steve Ebersole
 */
public interface InvalidatableWrapper<T> extends JdbcWrapper<T> {
	/**
	 * Make the wrapper invalid for further usage.
	 */
	public void invalidate();
}
