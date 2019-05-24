/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.named.spi;

import java.util.Set;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.NativeQueryImplementor;

/**
 * Descriptor for a named native query in the run-time environment
 *
 * @author Steve Ebersole
 */
public interface NamedNativeQueryMemento extends NamedQueryMemento {
	String getSqlString();

	String getResultSetMappingName();

	Set<String> getQuerySpaces();

	@Override
	NamedNativeQueryMemento makeCopy(String name);

	@Override
	<T> NativeQueryImplementor<T> toQuery(SharedSessionContractImplementor session, Class<T> resultType);
}
