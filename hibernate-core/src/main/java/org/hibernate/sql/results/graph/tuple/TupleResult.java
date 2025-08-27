/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.tuple;

import java.util.BitSet;

import org.hibernate.Internal;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.basic.BasicResultGraphNode;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Christian Beikov
 */
public class TupleResult<T> implements DomainResult<T>, BasicResultGraphNode<T> {
	private final String resultVariable;
	private final JavaType<T> javaType;

	private final NavigablePath navigablePath;

	private final TupleResultAssembler<T> assembler;

	public TupleResult(
			int[] jdbcValuesArrayPositions,
			String resultVariable,
			JavaType<T> javaType) {
		this( jdbcValuesArrayPositions, resultVariable, javaType, null );
	}

	public TupleResult(
			int[] jdbcValuesArrayPositions,
			String resultVariable,
			JavaType<T> javaType,
			NavigablePath navigablePath) {
		this.resultVariable = resultVariable;
		this.javaType = javaType;

		this.navigablePath = navigablePath;

		this.assembler = new TupleResultAssembler<>( jdbcValuesArrayPositions, javaType );
	}

	@Override
	public String getResultVariable() {
		return resultVariable;
	}

	@Override
	public JavaType<T> getResultJavaType() {
		return javaType;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	/**
	 * For testing purposes only
	 */
	@Internal
	public DomainResultAssembler<T> getAssembler() {
		return assembler;
	}

	@Override
	public DomainResultAssembler<T> createResultAssembler(
			InitializerParent<?> parent,
			AssemblerCreationState creationState) {
		return assembler;
	}

	@Override
	public void collectValueIndexesToCache(BitSet valueIndexes) {
		for ( int valuesArrayPosition : assembler.getValuesArrayPositions() ) {
			valueIndexes.set( valuesArrayPosition );
		}
	}
}
