/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.Type;

/**
 * Represent a simple scalar return within a query result.  Generally this would be values of basic (String, Integer,
 * etc) or composite types.
 *
 * @author Steve Ebersole
 * @author Gail Badner
 */
public interface ScalarReturn extends Return {

	/**
	 * Gets the name of the scalar return.
	 *
	 * @return The name of the scalar return.
	 */
	public String getName();

	/**
	 * Gets the type of the scalar return.
	 *
	 * @return The type of the scalar return.
	 */
	public Type getType();
}
