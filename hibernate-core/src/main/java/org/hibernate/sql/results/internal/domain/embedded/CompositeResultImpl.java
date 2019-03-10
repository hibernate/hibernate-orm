/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.embedded;

import java.util.function.Consumer;

import org.hibernate.metamodel.model.domain.spi.EmbeddedValuedNavigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.internal.domain.AbstractFetchParent;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.CompositeResult;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.Initializer;
import org.hibernate.type.descriptor.java.spi.EmbeddableJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public class CompositeResultImpl extends AbstractFetchParent implements CompositeResult {
	private final NavigablePath navigablePath;
	private final EmbeddedValuedNavigable navigable;
	private final String resultVariable;

	public CompositeResultImpl(
			NavigablePath navigablePath,
			EmbeddedValuedNavigable navigable,
			String resultVariable,
			DomainResultCreationState creationState) {
		super( navigable, navigablePath );
		this.resultVariable = resultVariable;
		this.navigablePath = navigablePath;
		this.navigable = navigable;

		afterInitialize( creationState );
	}

	@Override
	public String getResultVariable() {
		return resultVariable;
	}


	@Override
	public DomainResultAssembler createResultAssembler(
			Consumer<Initializer> initializerCollector,
			AssemblerCreationState creationState) {
		final CompositeRootInitializerImpl initializer = new CompositeRootInitializerImpl(
				null,
				this,
				initializerCollector,
				creationState
		);

		initializerCollector.accept( initializer );

		return new CompositeAssembler( initializer );
	}

	@Override
	public EmbeddedValuedNavigable getNavigableContainer() {
		return (EmbeddedValuedNavigable) super.getNavigableContainer();
	}

	@Override
	public EmbeddedValuedNavigable getCompositeNavigableDescriptor() {
		return getNavigableContainer();
	}

	@Override
	public EmbeddableJavaDescriptor getJavaTypeDescriptor() {
		return getCompositeNavigableDescriptor().getJavaTypeDescriptor();
	}
}
