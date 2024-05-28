/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
