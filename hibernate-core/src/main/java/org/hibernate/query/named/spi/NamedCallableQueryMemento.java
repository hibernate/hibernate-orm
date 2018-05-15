/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.named.spi;

import java.util.Set;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.spi.ProcedureCallImplementor;

/**
 * @author Steve Ebersole
 */
public interface NamedCallableQueryMemento extends NamedQueryMemento {
	String getCallableName();

	Set<String> getQuerySpaces();

	Class[] getResultClasses();

	String[] getResultSetMappingNames();

	@Override
	NamedCallableQueryMemento makeCopy(String name);

	@Override
	<T> ProcedureCallImplementor<T> toQuery(SharedSessionContractImplementor session, Class<T> resultType);
}
