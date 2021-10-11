/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.EntityUniqueKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.Association;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.UniqueKeyLoadable;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.BiDirectionalFetch;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Andrea Boriero
 */
public class CircularBiDirectionalFetchImpl implements BiDirectionalFetch, Association {
	private final FetchTiming timing;
	private final NavigablePath navigablePath;
	private final ToOneAttributeMapping fetchable;

	private final FetchParent fetchParent;
	private final LockMode lockMode;
	private final NavigablePath referencedNavigablePath;

	public CircularBiDirectionalFetchImpl(
			FetchTiming timing,
			NavigablePath navigablePath,
			FetchParent fetchParent,
			ToOneAttributeMapping fetchable,
			LockMode lockMode,
			NavigablePath referencedNavigablePath) {
		this.timing = timing;
		this.fetchParent = fetchParent;
		this.navigablePath = navigablePath;
		this.fetchable = fetchable;
		this.lockMode = lockMode;
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
	public JavaType<?> getResultJavaTypeDescriptor() {
		return fetchable.getJavaTypeDescriptor();
	}

	@Override
	public DomainResultAssembler createAssembler(
			FetchParentAccess parentAccess,
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
	public JavaType<?> getJavaTypeDescriptor() {
		return fetchable.getJavaTypeDescriptor();
	}

	@Override
	public FetchOptions getMappedFetchOptions() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ForeignKeyDescriptor getForeignKeyDescriptor() {
		return ( (Association) fetchParent ).getForeignKeyDescriptor();
	}

	@Override
	public ForeignKeyDescriptor.Nature getSideNature() {
		return ( (Association) fetchParent ).getSideNature();
	}

	@Override
	public void breakDownJdbcValues(Object domainValue, JdbcValueConsumer valueConsumer, SharedSessionContractImplementor session) {
		fetchable.breakDownJdbcValues( domainValue, valueConsumer, session );
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {
		throw new UnsupportedOperationException();
	}

	private static class CircularFetchAssembler implements DomainResultAssembler {
		private final NavigablePath circularPath;
		private final JavaType javaTypeDescriptor;
		private final ToOneAttributeMapping fetchable;

		public CircularFetchAssembler(
				ToOneAttributeMapping fetchable,
				NavigablePath circularPath,
				JavaType javaTypeDescriptor) {
			this.fetchable = fetchable;
			this.circularPath = circularPath;
			this.javaTypeDescriptor = javaTypeDescriptor;
		}

		@Override
		public Object assemble(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options) {
			EntityInitializer initializer = resolveCircularInitializer( rowProcessingState );
			if ( initializer == null ) {
				if ( circularPath.getParent() != null ) {
					NavigablePath path = circularPath.getParent();
					Initializer parentInitializer = rowProcessingState.resolveInitializer( path );
					while ( !( parentInitializer instanceof EntityInitializer ) && path.getParent() != null ) {
						path = path.getParent();
						parentInitializer = rowProcessingState.resolveInitializer( path );
					}
					initializer = (EntityInitializer) parentInitializer;
				}
				else {
					final Initializer parentInitializer = rowProcessingState.resolveInitializer( circularPath );
					assert parentInitializer instanceof CollectionInitializer;
					final CollectionInitializer circ = (CollectionInitializer) parentInitializer;
					final EntityPersister entityPersister = (EntityPersister) ( (AttributeMapping) fetchable ).getMappedType();
					final CollectionKey collectionKey = circ.resolveCollectionKey( rowProcessingState );
					final Object key = collectionKey.getKey();


					final SharedSessionContractImplementor session = rowProcessingState.getJdbcValuesSourceProcessingState()
							.getSession();
					final PersistenceContext persistenceContext = session.getPersistenceContext();
					if ( fetchable.getReferencedPropertyName() != null ) {
						return loadByUniqueKey( entityPersister, key, session, persistenceContext );
					}
					else {
						final EntityKey entityKey = new EntityKey( key, entityPersister );

						final Object proxy = persistenceContext.getProxy( entityKey );
						// it is conceivable there is a proxy, so check that first
						if ( proxy == null ) {
							// otherwise look for an initialized version
							return persistenceContext.getEntity( entityKey );
						}
						return proxy;
					}
				}
			}
			if ( initializer.getInitializedInstance() == null ) {
				initializer.resolveKey( rowProcessingState );
				initializer.resolveInstance( rowProcessingState );
			}
			return initializer.getInitializedInstance();
		}

		private Object loadByUniqueKey(
				EntityPersister entityPersister,
				Object key,
				SharedSessionContractImplementor session,
				PersistenceContext persistenceContext) {
			String uniqueKeyPropertyName = fetchable.getReferencedPropertyName();
			EntityUniqueKey euk = new EntityUniqueKey(
					entityPersister.getEntityName(),
					uniqueKeyPropertyName,
					key,
					entityPersister.getIdentifierType(),
					entityPersister.getEntityMode(),
					session.getFactory()
			);
			Object entityInstance = persistenceContext.getEntity( euk );
			if ( entityInstance == null ) {
				entityInstance = ( (UniqueKeyLoadable) entityPersister ).loadByUniqueKey(
						uniqueKeyPropertyName,
						key,
						session
				);

				// If the entity was not in the Persistence Context, but was found now,
				// add it to the Persistence Context
				if ( entityInstance != null ) {
					persistenceContext.addEntity( euk, entityInstance );
				}
			}
			return entityInstance;
		}

		private EntityInitializer resolveCircularInitializer(RowProcessingState rowProcessingState) {
			final Initializer initializer = rowProcessingState.resolveInitializer( circularPath );
			if ( initializer instanceof EntityInitializer ) {
				return (EntityInitializer) initializer;
			}
			if ( initializer instanceof CollectionInitializer ) {
				return null;
			}
			final ModelPart initializedPart = initializer.getInitializedPart();

			if ( initializedPart instanceof EntityInitializer ) {
				return (EntityInitializer) initializedPart;
			}

			NavigablePath path = circularPath.getParent();
			Initializer parentInitializer = rowProcessingState.resolveInitializer( path );
			while ( !( parentInitializer instanceof EntityInitializer ) && path.getParent() != null ) {
				path = path.getParent();
				parentInitializer = rowProcessingState.resolveInitializer( path );
			}

			if ( !( parentInitializer instanceof EntityInitializer ) ) {
				return null;
			}

			return (EntityInitializer) parentInitializer;
		}

		@Override
		public JavaType getAssembledJavaTypeDescriptor() {
			return javaTypeDescriptor;
		}
	}

}
