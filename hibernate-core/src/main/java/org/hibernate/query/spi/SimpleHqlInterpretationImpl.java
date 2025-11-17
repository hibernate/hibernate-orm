/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.Internal;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;

import static org.hibernate.query.sqm.internal.SqmUtil.checkQueryReturnType;
import static org.hibernate.query.sqm.internal.SqmUtil.isResultTypeAlwaysAllowed;

/**
 * Default implementation if {@link HqlInterpretation}.
 *
 * @apiNote This class is now considered internal implementation
 * and will move to an internal package in a future version.
 * Application programs should never depend directly on this class.
 *
 * @author Steve Ebersole
 */
@Internal
public class SimpleHqlInterpretationImpl<R> implements HqlInterpretation<R> {
	private final SqmStatement<R> sqmStatement;
	private final ParameterMetadataImplementor parameterMetadata;
	private final DomainParameterXref domainParameterXref;
	private final ConcurrentHashMap<Class<?>, Object> allowedReturnTypes;

	public SimpleHqlInterpretationImpl(
			SqmStatement<R> sqmStatement,
			ParameterMetadataImplementor parameterMetadata,
			DomainParameterXref domainParameterXref) {
		this.sqmStatement = sqmStatement;
		this.parameterMetadata = parameterMetadata;
		this.domainParameterXref = domainParameterXref;
		this.allowedReturnTypes = new ConcurrentHashMap<>();
	}

	@Override
	public SqmStatement<R> getSqmStatement() {
		return sqmStatement;
	}

	@Override
	public ParameterMetadataImplementor getParameterMetadata() {
		return parameterMetadata;
	}

	@Override
	public DomainParameterXref getDomainParameterXref() {
		return domainParameterXref.copy();
	}

	@Override
	public void validateResultType(Class<?> resultType) {
		assert sqmStatement instanceof SqmSelectStatement<?>;
		if ( resultType != null && !isResultTypeAlwaysAllowed( resultType ) ) {
			if ( !allowedReturnTypes.containsKey( resultType ) ) {
				checkQueryReturnType( ( (SqmSelectStatement<R>) sqmStatement ).getQueryPart(), resultType );
				allowedReturnTypes.put( resultType, Boolean.TRUE );
			}
		}
	}
}
