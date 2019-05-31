/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.result.spi;

import java.util.Set;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.exec.spi.DomainParameterBindingContext;

/**
 * @author Steve Ebersole
 */
public interface ResultContext {
	SharedSessionContractImplementor getSession();

	Set<String> getSynchronizedQuerySpaces();

	QueryOptions getQueryOptions();

	DomainParameterBindingContext getDomainParameterBindingContext();
}
