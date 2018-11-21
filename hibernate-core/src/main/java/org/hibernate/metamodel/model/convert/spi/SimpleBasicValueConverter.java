/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.convert.spi;

import java.util.function.BiFunction;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public class SimpleBasicValueConverter<D,R> implements BasicValueConverter<D,R> {
	private final BasicJavaDescriptor<D> domainJtd;
	private final BasicJavaDescriptor<R> relationalJtd;

	private final BiFunction<R,SharedSessionContractImplementor,D> toDomainHandler;
	private final BiFunction<D,SharedSessionContractImplementor,R> toRelationalHandler;

	public SimpleBasicValueConverter(
			BasicJavaDescriptor<D> domainJtd,
			BasicJavaDescriptor<R> relationalJtd,
			BiFunction<R,SharedSessionContractImplementor,D> toDomainHandler,
			BiFunction<D,SharedSessionContractImplementor,R> toRelationalHandler) {
		this.domainJtd = domainJtd;
		this.relationalJtd = relationalJtd;
		this.toDomainHandler = toDomainHandler;
		this.toRelationalHandler = toRelationalHandler;
	}

	@Override
	public D toDomainValue(R relationalForm, SharedSessionContractImplementor session) {
		return toDomainHandler.apply( relationalForm, session );
	}

	@Override
	public R toRelationalValue(D domainForm, SharedSessionContractImplementor session) {
		return toRelationalHandler.apply( domainForm, session );
	}

	@Override
	public BasicJavaDescriptor<D> getDomainJavaDescriptor() {
		return domainJtd;
	}

	@Override
	public BasicJavaDescriptor<R> getRelationalJavaDescriptor() {
		return relationalJtd;
	}
}
