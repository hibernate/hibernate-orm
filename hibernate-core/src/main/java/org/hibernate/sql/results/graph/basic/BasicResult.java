/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.basic;

import java.util.BitSet;

import org.hibernate.Internal;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * DomainResult for a basic-value
 *
 * @author Steve Ebersole
 */
public class BasicResult<T> implements DomainResult<T>, BasicResultGraphNode<T> {
	private final String resultVariable;
	private final JavaType<T> javaType;

	private final NavigablePath navigablePath;

	private final BasicResultAssembler<T> assembler;

	public BasicResult(
			int jdbcValuesArrayPosition,
			String resultVariable,
			JdbcMapping jdbcMapping) {
		this(
				jdbcValuesArrayPosition,
				resultVariable,
				jdbcMapping,
				null,
				false,
				false
		);
	}

	public BasicResult(
			int jdbcValuesArrayPosition,
			String resultVariable,
			JdbcMapping jdbcMapping,
			NavigablePath navigablePath,
			boolean coerceResultType,
			boolean unwrapRowProcessingState) {
		//noinspection unchecked
		this(
				jdbcValuesArrayPosition,
				resultVariable,
				(JavaType<T>) jdbcMapping.getJavaTypeDescriptor(),
				(BasicValueConverter<T,?>) jdbcMapping.getValueConverter(),
				navigablePath,
				coerceResultType,
				unwrapRowProcessingState
		);
	}

	public BasicResult(
			int valuesArrayPosition,
			String resultVariable,
			JavaType<T> javaType,
			BasicValueConverter<T,?> converter,
			NavigablePath navigablePath,
			boolean coerceResultType,
			boolean unwrapRowProcessingState) {
		this.resultVariable = resultVariable;
		this.javaType = javaType;
		this.navigablePath = navigablePath;
		this.assembler = assembler( valuesArrayPosition, javaType, converter, coerceResultType, unwrapRowProcessingState );
	}

	private static <T> BasicResultAssembler<T> assembler(
			int valuesArrayPosition,
			JavaType<T> javaType,
			BasicValueConverter<T, ?> converter,
			boolean coerceResultType,
			boolean unwrapRowProcessingState) {
		return coerceResultType
				? new CoercingResultAssembler<>( valuesArrayPosition, javaType, converter, unwrapRowProcessingState )
				: new BasicResultAssembler<>( valuesArrayPosition, javaType, converter, unwrapRowProcessingState );
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
		valueIndexes.set( assembler.valuesArrayPosition );
	}
}
