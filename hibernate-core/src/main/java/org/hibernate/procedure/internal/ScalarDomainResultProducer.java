/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.procedure.internal;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.sql.results.internal.ScalarDomainResultImpl;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.DomainResultProducer;

/**
 * @author Steve Ebersole
 */
public class ScalarDomainResultProducer<T> implements DomainResultProducer<T> {
	private final SqmExpressable<T> expressableType;

	public ScalarDomainResultProducer(SqmExpressable<T> expressableType) {
		this.expressableType = expressableType;
	}

	@Override
	public DomainResult<T> createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState) {
		throw new NotYetImplementedFor6Exception( getClass() );
//		//noinspection unchecked
//		return new ScalarDomainResultImpl( resultVariable, expressableType.getExpressableJavaTypeDescriptor() );
	}
}
