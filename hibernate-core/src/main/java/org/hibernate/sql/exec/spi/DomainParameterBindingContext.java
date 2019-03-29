/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.spi;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryAccess;
import org.hibernate.query.spi.QueryParameterBindings;

/**
 * Contextual information for performing JDBC parameter binding.  Generally
 * speaking this is the source of all bind values in terms of the domain model
 *
 * @author Steve Ebersole
 */
public interface DomainParameterBindingContext extends SessionFactoryAccess {
	<T> List<T> getLoadIdentifiers();

	QueryParameterBindings getQueryParameterBindings();
}
