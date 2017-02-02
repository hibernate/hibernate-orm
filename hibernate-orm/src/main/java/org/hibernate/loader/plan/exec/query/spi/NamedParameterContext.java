/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.exec.query.spi;

/**
 * The context for named parameters.
 * <p/>
 * NOTE : the hope with the SQL-redesign stuff is that this whole concept goes away, the idea being that
 * the parameters are encoded into the query tree and "bind themselves"; see {@link org.hibernate.param.ParameterSpecification}.
 *
 * @author Steve Ebersole
 */
public interface NamedParameterContext {
	/**
	 * Returns the locations of all occurrences of the named parameter.
	 *
	 * @param name The named parameter.
	 *
	 * @return the array of locations.
	 */
	public int[] getNamedParameterLocations(String name);
}
