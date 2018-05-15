/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.annotations.Remove;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;

/**
 * @author Steve Ebersole
 */
public interface AllowableParameterType<J> extends ExpressableType<J> {
	/**
	 * The number of JDBC parameters needed for this type.
	 * @deprecated To be removed.
	 */
	@Deprecated
	@Remove
	int getNumberOfJdbcParametersNeeded();

}
