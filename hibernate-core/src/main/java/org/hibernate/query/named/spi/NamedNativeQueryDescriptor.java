/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.named.spi;

import java.util.Collection;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.NativeQueryImplementor;

/**
 * Descriptor for a named native query in the run-time environment
 *
 * @author Steve Ebersole
 */
public interface NamedNativeQueryDescriptor extends NamedQueryDescriptor {
	String getSqlString();
	String getResultSetMappingName();

	Collection<String> getQuerySpaces();

	@Override
	NativeQueryImplementor toQuery(SharedSessionContractImplementor session);

	NamedNativeQueryDescriptor makeCopy(String name);
}
