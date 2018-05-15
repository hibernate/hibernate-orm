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
import org.hibernate.sql.results.spi.AssemblerCreationContext;
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
	private final String resultVariable;
	private final EmbeddedValuedNavigable navigable;

	public CompositeResultImpl(
			String resultVariable,
			EmbeddedValuedNavigable navigable,
			DomainResultCreationState creationState) {
		super( navigable, new NavigablePath( navigable.getEmbeddedDescriptor().getRoleName() ) );
		this.resultVariable = resultVariable;
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
			AssemblerCreationState creationOptions,
			AssemblerCreationContext creationContext) {
		final CompositeRootInitializerImpl initializer = new CompositeRootInitializerImpl(
				null,
				this,
				initializerCollector,
				creationOptions,
				creationContext
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
