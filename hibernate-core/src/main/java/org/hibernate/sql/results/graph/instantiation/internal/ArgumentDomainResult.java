/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.instantiation.internal;

import java.util.BitSet;

import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class ArgumentDomainResult<A> implements DomainResult<A> {
	private final DomainResult<A> realDomainResult;

	public ArgumentDomainResult(DomainResult<A> realDomainResult) {
		this.realDomainResult = realDomainResult;
	}

	@Override
	public String getResultVariable() {
		return realDomainResult.getResultVariable();
	}

	@Override
	public boolean containsAnyNonScalarResults() {
		return realDomainResult.containsAnyNonScalarResults();
	}

	@Override
	@SuppressWarnings("rawtypes")
	public JavaType<?> getResultJavaType() {
		return realDomainResult.getResultJavaType();
	}

	@Override
	public ArgumentReader<A> createResultAssembler(InitializerParent<?> parent, AssemblerCreationState creationState) {
		return new ArgumentReader<>(
				realDomainResult.createResultAssembler( parent, creationState ),
				getResultVariable()
		);
	}

	@Override
	public void collectValueIndexesToCache(BitSet valueIndexes) {
		realDomainResult.collectValueIndexesToCache( valueIndexes );
	}
}
