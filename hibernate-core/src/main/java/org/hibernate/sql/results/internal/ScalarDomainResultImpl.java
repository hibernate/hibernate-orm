/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import java.util.function.Consumer;

import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.Initializer;
import org.hibernate.sql.results.spi.ScalarDomainResult;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class ScalarDomainResultImpl<T> implements ScalarDomainResult<T> {
	private final String resultVariable;
	private final JavaTypeDescriptor<T> javaTypeDescriptor;

	private final DomainResultAssembler<T> assembler;

	public ScalarDomainResultImpl(
			int jdbcValuesArrayPosition,
			String resultVariable,
			JavaTypeDescriptor<T> javaTypeDescriptor) {
		this.resultVariable = resultVariable;
		this.javaTypeDescriptor = javaTypeDescriptor;

		this.assembler = new BasicResultAssembler<>( jdbcValuesArrayPosition, javaTypeDescriptor );
	}

	public ScalarDomainResultImpl(
			int valuesArrayPosition,
			String resultVariable,
			JavaTypeDescriptor<T> javaTypeDescriptor,
			BasicValueConverter<T,?> valueConverter) {
		this.resultVariable = resultVariable;
		this.javaTypeDescriptor = javaTypeDescriptor;

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
	public DomainResultAssembler<T> createResultAssembler(
			Consumer<Initializer> initializerCollector,
			AssemblerCreationState creationState) {
		return assembler;
	}
}
