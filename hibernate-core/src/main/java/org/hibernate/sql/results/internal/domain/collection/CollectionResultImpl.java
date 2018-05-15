/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.collection;

import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.spi.AssemblerCreationContext;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.CollectionInitializer;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.Initializer;
import org.hibernate.sql.results.spi.CollectionResult;

/**
 * @author Steve Ebersole
 */
public class CollectionResultImpl
		extends AbstractCollectionMappingNode
		implements CollectionResult {

	private final CollectionInitializerProducer initializerProducer;
	private final NavigablePath navigablePath;
	private final LockMode lockMode;

	public CollectionResultImpl(
			PluralPersistentAttribute attributeDescriptor,
			NavigablePath navigablePath,
			String resultVariable,
			LockMode lockMode,
			DomainResult keyResult,
			CollectionInitializerProducer initializerProducer) {
		super(
				null,
				attributeDescriptor,
				resultVariable,
				keyResult,
				null
		);
		this.navigablePath = navigablePath;
		this.lockMode = lockMode;
		this.initializerProducer = initializerProducer;
	}

	public LockMode getLockMode() {
		return lockMode;
	}

	@Override
	public DomainResultAssembler createResultAssembler(
			Consumer<Initializer> initializerCollector,
			AssemblerCreationState creationOptions,
			AssemblerCreationContext creationContext) {
		final CollectionInitializer initializer = initializerProducer.produceInitializer(
				null,
				navigablePath,
				getLockMode(),
				getKeyContainerResult().createResultAssembler( initializerCollector, creationOptions, creationContext ),
				null,
				initializerCollector,
				creationOptions,
				creationContext
		);

		initializerCollector.accept( initializer );

		return new PluralAttributeAssemblerImpl( initializer );
	}
}
