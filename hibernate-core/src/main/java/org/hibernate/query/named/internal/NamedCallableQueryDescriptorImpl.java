/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.named.internal;

import java.util.Collection;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.spi.ProcedureCallImplementor;
import org.hibernate.query.named.spi.AbstractNamedQueryDescriptor;
import org.hibernate.query.named.spi.NamedCallableQueryDescriptor;

/**
 * @author Steve Ebersole
 */
public class NamedCallableQueryDescriptorImpl
		extends AbstractNamedQueryDescriptor
		implements NamedCallableQueryDescriptor {
	private final String callableName;
	private final Collection<String> querySpaces;

	public NamedCallableQueryDescriptorImpl(
			String name,
			String callableName,
			Collection<String> querySpaces,
			Boolean cacheable,
			String cacheRegion,
			CacheMode cacheMode,
			FlushMode flushMode,
			Boolean readOnly,
			LockOptions lockOptions,
			Integer timeout,
			Integer fetchSize,
			String comment) {
		super(
				name,
				cacheable,
				cacheRegion,
				cacheMode,
				flushMode,
				readOnly,
				lockOptions,
				timeout,
				fetchSize,
				comment
		);
		this.callableName = callableName;
		this.querySpaces = querySpaces;

	}

	@Override
	public String getCallableName() {
		return callableName;
	}

	@Override
	public Collection<String> getQuerySpaces() {
		return querySpaces;
	}

	@Override
	public String getQueryString() {
		return callableName;
	}

	@Override
	public ProcedureCallImplementor toQuery(SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception();
	}
}
