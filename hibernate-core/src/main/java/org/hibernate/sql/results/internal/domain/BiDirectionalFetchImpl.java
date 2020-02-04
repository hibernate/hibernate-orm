/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain;

import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.Association;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.SingularAssociationAttributeMapping;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.BiDirectionalFetch;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;
import org.hibernate.sql.results.graph.collection.internal.AbstractCollectionInitializer;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.graph.entity.LoadingEntityEntry;
import org.hibernate.sql.results.graph.entity.internal.EntityResultImpl;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Andrea Boriero
 */
public class BiDirectionalFetchImpl implements BiDirectionalFetch, Association {
	private final FetchTiming timing;
	private final NavigablePath navigablePath;
	private final Fetchable fetchable;

	private final FetchParent fetchParent;
	private final NavigablePath referencedNavigablePath;

	public BiDirectionalFetchImpl(
			FetchTiming timing,
			NavigablePath navigablePath,
			FetchParent fetchParent,
			Fetchable fetchable,
			NavigablePath referencedNavigablePath) {
		this.timing = timing;
		this.fetchParent = fetchParent;
		this.navigablePath = navigablePath;
		this.fetchable = fetchable;
		this.referencedNavigablePath = referencedNavigablePath;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public NavigablePath getReferencedPath() {
		return referencedNavigablePath;
	}

	@Override
	public FetchParent getFetchParent() {
		return fetchParent;
	}

	@Override
	public Fetchable getFetchedMapping() {
		return fetchable;
	}

	@Override
	public JavaTypeDescriptor getResultJavaTypeDescriptor() {
		return fetchable.getJavaTypeDescriptor();
	}

	@Override
	public boolean isNullable() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public DomainResultAssembler createAssembler(
			FetchParentAccess parentAccess,
			Consumer<Initializer> collector,
			AssemblerCreationState creationState) {
		return new CircularFetchAssembler(
				fetchable,
				getReferencedPath(),
				fetchable.getJavaTypeDescriptor()
		);
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
	public String getFetchableName() {
		return fetchable.getFetchableName();
	}

	@Override
	public String getPartName() {
		return fetchable.getFetchableName();
	}

	@Override
	public NavigableRole getNavigableRole() {
		return fetchable.getNavigableRole();
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return fetchable.findContainingEntityMapping();
	}

	@Override
	public MappingType getPartMappingType() {
		return fetchable.getPartMappingType();
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return fetchable.getJavaTypeDescriptor();
	}

	@Override
	public FetchStrategy getMappedFetchStrategy() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ForeignKeyDescriptor getForeignKeyDescriptor() {
		return ( (Association) fetchParent ).getForeignKeyDescriptor();
	}

	@Override
	public String[] getIdentifyingColumnExpressions() {
		// fetch parent really always needs to be an Association, so we simply cast here
		//		should maybe verify this in ctor
		return ( (Association) fetchParent ).getIdentifyingColumnExpressions();
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			LockMode lockMode,
			String resultVariable,
			DomainResultCreationState creationState) {
		throw new UnsupportedOperationException();
	}

	private static class CircularFetchAssembler implements DomainResultAssembler {
		private final NavigablePath circularPath;
		private final JavaTypeDescriptor javaTypeDescriptor;
		private final Fetchable fetchable;

		public CircularFetchAssembler(
				Fetchable fetchable,
				NavigablePath circularPath,
				JavaTypeDescriptor javaTypeDescriptor) {
			this.fetchable = fetchable;
			this.circularPath = circularPath;
			this.javaTypeDescriptor = javaTypeDescriptor;
		}

		@Override
		public Object assemble(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options) {
			final EntityInitializer initializer = resolveCircularInitializer( rowProcessingState );
			if ( initializer == null ) {

				final Initializer parentInitializer = rowProcessingState.resolveInitializer(
						circularPath.getParent() );
				assert parentInitializer instanceof CollectionInitializer;
				final CollectionInitializer circ = (CollectionInitializer) parentInitializer;
				final CollectionKey collectionKey = circ.resolveCollectionKey( rowProcessingState );
				final EntityKey entityKey = new EntityKey(
						collectionKey.getKey(),
						(EntityPersister) ( (AttributeMapping) fetchable ).getMappedTypeDescriptor()
				);

				final SharedSessionContractImplementor session = rowProcessingState.getJdbcValuesSourceProcessingState()
						.getSession();
				return session.getPersistenceContext()
						.getEntity( entityKey );

			}
			if ( initializer.getInitializedInstance() == null ) {
				initializer.resolveKey( rowProcessingState );
				initializer.resolveInstance( rowProcessingState );
				initializer.initializeInstance( rowProcessingState );
			}
			return initializer.getInitializedInstance();
		}

		private EntityInitializer resolveCircularInitializer(RowProcessingState rowProcessingState) {
			final Initializer initializer = rowProcessingState.resolveInitializer( circularPath );
			final ModelPart initializedPart = initializer.getInitializedPart();

			if ( initializedPart instanceof EntityInitializer ) {
				return (EntityInitializer) initializedPart;
			}

			NavigablePath path = circularPath.getParent();
			Initializer parentInitializer = rowProcessingState.resolveInitializer( path );
			while ( !( parentInitializer instanceof EntityInitializer) && path.getParent() != null ) {
				path = path.getParent();
				parentInitializer = rowProcessingState.resolveInitializer( path );

			}

			if ( !( parentInitializer instanceof EntityInitializer ) ) {
				return null;
			}

			return (EntityInitializer) parentInitializer;
		}

		@Override
		public JavaTypeDescriptor getAssembledJavaTypeDescriptor() {
			return javaTypeDescriptor;
		}
	}

}
