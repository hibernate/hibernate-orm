/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.consume.spi;

import java.util.Collection;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.QueryParameterBindings;

/**
 * Contextual information for performing JDBC parameter binding.  Generally
 * speaking this is the source of all bind values
 *
 * @author Steve Ebersole
 */
public interface ParameterBindingContext {
	// todo (6.0) : consider more deeply this `#getLoadIdentifier` solution.  seems like there must be a better way.

	<T> Collection<T> getLoadIdentifiers();

	QueryParameterBindings getQueryParameterBindings();

	SharedSessionContractImplementor getSession();
}
