/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain;

import java.util.BitSet;

import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.BiDirectionalFetch;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerData;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaType;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @author Andrea Boriero
 */
public class CircularBiDirectionalFetchImpl implements BiDirectionalFetch {
	private final NavigablePath navigablePath;
	private final ToOneAttributeMapping fetchedModelPart;
	private final FetchParent fetchParent;
	private final @Nullable DomainResult<?> keyResult;
	private final FetchTiming timing;
	private final NavigablePath referencedNavigablePath;

	public CircularBiDirectionalFetchImpl(
			FetchTiming timing,
			NavigablePath navigablePath,
			FetchParent fetchParent,
			ToOneAttributeMapping referencedModelPart,
			NavigablePath referencedNavigablePath,
			@Nullable DomainResult<?> keyResult) {
		this.navigablePath = navigablePath;
		this.fetchedModelPart = referencedModelPart;
		this.fetchParent = fetchParent;
		this.keyResult = keyResult;
		this.timing = timing;
		this.referencedNavigablePath = referencedNavigablePath;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public ToOneAttributeMapping getFetchedMapping() {
		return fetchedModelPart;
	}

	@Override
	public JavaType<?> getResultJavaType() {
		return fetchedModelPart.getJavaType();
	}

	@Override
	public FetchParent getFetchParent() {
		return fetchParent;
	}

	@Override
	public NavigablePath getReferencedPath() {
		return referencedNavigablePath;
	}

	@Override
	public FetchTiming getTiming() {
		return timing;
	}

	@Override
	public boolean hasTableGroup() {
		return true;
	}

	@Override
	public void collectValueIndexesToCache(BitSet valueIndexes) {
		if ( keyResult != null ) {
			keyResult.collectValueIndexesToCache( valueIndexes );
		}
	}

	@Override
	public DomainResultAssembler createAssembler(InitializerParent<?> parent, AssemblerCreationState creationState) {
		return new CircularBiDirectionalFetchAssembler(
				getResultJavaType(),
				keyResult == null ? null : keyResult.createResultAssembler( parent, creationState ),
				resolveCircularInitializer( parent )
		);
	}

	private EntityInitializer<?> resolveCircularInitializer(InitializerParent<?> parent) {
		while (parent != null && getReferencedPath().isParent( parent.getNavigablePath() ) ) {
			parent = parent.getParent();
		}
		assert parent instanceof EntityInitializer && parent.getNavigablePath().equals( getReferencedPath() );
		return parent.asEntityInitializer();
	}

	private static class CircularBiDirectionalFetchAssembler implements DomainResultAssembler<Object> {
		private final JavaType<Object> javaType;
		private final @Nullable DomainResultAssembler<?> keyDomainResultAssembler;
		private final EntityInitializer<InitializerData> initializer;

		public CircularBiDirectionalFetchAssembler(
				JavaType<?> javaType,
				@Nullable DomainResultAssembler<?> keyDomainResultAssembler,
				EntityInitializer<?> initializer) {
			//noinspection unchecked
			this.javaType = (JavaType<Object>) javaType;
			this.keyDomainResultAssembler = keyDomainResultAssembler;
			this.initializer = (EntityInitializer<InitializerData>) initializer;
		}

		@Override
		public Object assemble(RowProcessingState rowProcessingState) {
			if ( keyDomainResultAssembler != null ) {
				final Object foreignKey = keyDomainResultAssembler.assemble( rowProcessingState );
				if ( foreignKey == null ) {
					return null;
				}
			}
			final InitializerData data = initializer.getData( rowProcessingState );
			initializer.resolveInstance( data );
			final Object initializedInstance = initializer.getEntityInstance( data );
			final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( initializedInstance );
			if ( lazyInitializer != null ) {
				final Class<?> concreteProxyClass = initializer.getConcreteDescriptor( data ).getConcreteProxyClass();
				if ( concreteProxyClass.isInstance( initializedInstance ) ) {
					return initializedInstance;
				}
				else {
					initializer.initializeInstance( rowProcessingState );
					return lazyInitializer.getImplementation();
				}
			}
			return initializedInstance;
		}

		@Override
		public void resolveState(RowProcessingState rowProcessingState) {
			if ( keyDomainResultAssembler != null ) {
				keyDomainResultAssembler.resolveState( rowProcessingState );
			}
		}

		@Override
		public @Nullable Initializer<?> getInitializer() {
			return keyDomainResultAssembler == null ? null : keyDomainResultAssembler.getInitializer();
		}

		@Override
		public JavaType<Object> getAssembledJavaType() {
			return javaType;
		}
	}

}
