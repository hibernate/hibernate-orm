/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.basic;

import java.util.function.Consumer;

import org.hibernate.Internal;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.Initializer;
import org.hibernate.sql.results.spi.BasicResultMappingNode;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class BasicResult<T> implements BasicResultMappingNode<T> {
	private final String resultVariable;
	private final JavaTypeDescriptor<T> javaTypeDescriptor;

	private final NavigablePath navigablePath;

	private final DomainResultAssembler<T> assembler;

	public BasicResult(
			int jdbcValuesArrayPosition,
			String resultVariable,
			JavaTypeDescriptor<T> javaTypeDescriptor) {
		this( jdbcValuesArrayPosition, resultVariable, javaTypeDescriptor, (NavigablePath) null );
	}

	public BasicResult(
			int jdbcValuesArrayPosition,
			String resultVariable,
			JavaTypeDescriptor<T> javaTypeDescriptor,
			NavigablePath navigablePath) {
		this.resultVariable = resultVariable;
		this.javaTypeDescriptor = javaTypeDescriptor;

		this.navigablePath = navigablePath;

		this.assembler = new BasicResultAssembler<>( jdbcValuesArrayPosition, javaTypeDescriptor );
	}

	public BasicResult(
			int valuesArrayPosition,
			String resultVariable,
			JavaTypeDescriptor<T> javaTypeDescriptor,
			BasicValueConverter<T,?> valueConverter) {
		this( valuesArrayPosition, resultVariable, javaTypeDescriptor, valueConverter, null );
	}

	public BasicResult(
			int valuesArrayPosition,
			String resultVariable,
			JavaTypeDescriptor<T> javaTypeDescriptor,
			BasicValueConverter<T,?> valueConverter,
			NavigablePath navigablePath) {
		this.resultVariable = resultVariable;
		this.javaTypeDescriptor = javaTypeDescriptor;
		this.navigablePath = navigablePath;

		this.assembler = new BasicResultAssembler<>( valuesArrayPosition, javaTypeDescriptor, valueConverter );
	}

	@Override
	public String getResultVariable() {
		return resultVariable;
	}

	@Override
	public JavaTypeDescriptor getResultJavaTypeDescriptor() {
		return javaTypeDescriptor;
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
			Consumer<Initializer> initializerCollector,
			AssemblerCreationState creationState) {
		return assembler;
	}
}
