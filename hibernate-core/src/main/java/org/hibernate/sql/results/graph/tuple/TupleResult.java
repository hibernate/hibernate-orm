/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.tuple;

import org.hibernate.Internal;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.basic.BasicResultGraphNode;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Christian Beikov
 */
public class TupleResult<T> implements DomainResult<T>, BasicResultGraphNode<T> {
	private final String resultVariable;
	private final JavaType<T> javaTypeDescriptor;

	private final NavigablePath navigablePath;

	private final DomainResultAssembler<T> assembler;

	public TupleResult(
			int[] jdbcValuesArrayPositions,
			String resultVariable,
			JavaType<T> javaTypeDescriptor) {
		this( jdbcValuesArrayPositions, resultVariable, javaTypeDescriptor, (NavigablePath) null );
	}

	public TupleResult(
			int[] jdbcValuesArrayPositions,
			String resultVariable,
			JavaType<T> javaTypeDescriptor,
			NavigablePath navigablePath) {
		this.resultVariable = resultVariable;
		this.javaTypeDescriptor = javaTypeDescriptor;

		this.navigablePath = navigablePath;

		this.assembler = new TupleResultAssembler<>( jdbcValuesArrayPositions, javaTypeDescriptor );
	}

	@Override
	public String getResultVariable() {
		return resultVariable;
	}

	@Override
	public JavaType<T> getResultJavaTypeDescriptor() {
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
			FetchParentAccess parentAccess,
			AssemblerCreationState creationState) {
		return assembler;
	}
}
