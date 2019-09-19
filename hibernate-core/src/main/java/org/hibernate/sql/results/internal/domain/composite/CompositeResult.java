/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.composite;

import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.internal.domain.AbstractFetchParent;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.CompositeResultMappingNode;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.Initializer;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class CompositeResult<T> extends AbstractFetchParent implements CompositeResultMappingNode, DomainResult<T> {
	private final String resultVariable;

	public CompositeResult(
			NavigablePath navigablePath,
			EmbeddableValuedModelPart modelPart,
			String resultVariable,
			DomainResultCreationState creationState) {
		super( modelPart, navigablePath );
		this.resultVariable = resultVariable;

		afterInitialize( creationState );
	}

	@Override
	public String getResultVariable() {
		return resultVariable;
	}

	@Override
	public EmbeddableValuedModelPart getFetchContainer() {
		return (EmbeddableValuedModelPart) super.getFetchContainer();
	}

	@Override
	public JavaTypeDescriptor getResultJavaTypeDescriptor() {
		return getReferencedMappingType().getJavaTypeDescriptor();
	}

	@Override
	public EmbeddableMappingType getReferencedMappingType() {
		return getFetchContainer().getEmbeddableTypeDescriptor();
	}

	@Override
	public EmbeddableValuedModelPart getReferencedMappingContainer() {
		return getFetchContainer();
	}

	@Override
	public DomainResultAssembler<T> createResultAssembler(
			Consumer<Initializer> initializerCollector,
			AssemblerCreationState creationState) {
		final CompositeRootInitializer initializer = new CompositeRootInitializer(
				this,
				initializerCollector,
				creationState
		);

		initializerCollector.accept( initializer );

		//noinspection unchecked
		return new CompositeAssembler( initializer );
	}
}
