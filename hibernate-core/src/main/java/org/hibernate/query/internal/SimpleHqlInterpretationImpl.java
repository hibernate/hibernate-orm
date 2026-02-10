/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.query.spi.HqlInterpretation;
import org.hibernate.query.spi.ParameterMetadataImplementor;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;

import static org.hibernate.query.sqm.internal.SqmUtil.checkQueryReturnType;
import static org.hibernate.query.sqm.internal.SqmUtil.isResultTypeAlwaysAllowed;

/**
 * Default implementation if {@link HqlInterpretation}.
 *
 * @author Steve Ebersole
 */
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
