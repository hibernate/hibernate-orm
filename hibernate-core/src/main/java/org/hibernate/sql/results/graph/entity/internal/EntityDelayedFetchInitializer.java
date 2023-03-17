/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.util.function.Consumer;

import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.EntityUniqueKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.UniqueKeyLoadable;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AbstractFetchParentAccess;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.graph.entity.LoadingEntityEntry;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.Type;

/**
 * @author Andrea Boriero
 * @author Steve Ebersole
 */
public class EntityDelayedFetchInitializer extends AbstractFetchParentAccess implements EntityInitializer {

	private final FetchParentAccess parentAccess;
	private final NavigablePath navigablePath;
	private final ToOneAttributeMapping referencedModelPart;
	private final boolean selectByUniqueKey;
	private final DomainResultAssembler<?> identifierAssembler;

	private Object entityInstance;
	private Object identifier;

	public EntityDelayedFetchInitializer(
			FetchParentAccess parentAccess,
			NavigablePath fetchedNavigable,
			ToOneAttributeMapping referencedModelPart,
			boolean selectByUniqueKey,
			DomainResultAssembler<?> identifierAssembler) {
		// associations marked with `@NotFound` are ALWAYS eagerly fetched
		assert referencedModelPart.getNotFoundAction() == null;

		this.parentAccess = parentAccess;
		this.navigablePath = fetchedNavigable;
		this.referencedModelPart = referencedModelPart;
		this.selectByUniqueKey = selectByUniqueKey;
		this.identifierAssembler = identifierAssembler;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public void resolveKey(RowProcessingState rowProcessingState) {
		// nothing to do
	}

	@Override
	public ModelPart getInitializedPart() {
		return referencedModelPart;
	}

	@Override
	public void resolveInstance(RowProcessingState rowProcessingState) {
		if ( entityInstance != null ) {
			return;
		}

		final EntityInitializer parentEntityInitializer = getParentEntityInitializer( parentAccess );
		if ( parentEntityInitializer != null && parentEntityInitializer.isEntityInitialized() ) {
			return;
		}

		if ( !isAttributeAssignableToConcreteDescriptor( parentAccess, referencedModelPart ) ) {
			return;
		}

		identifier = identifierAssembler.assemble( rowProcessingState );

		if ( identifier == null ) {
			entityInstance = null;
		}
		else {
			final SharedSessionContractImplementor session = rowProcessingState.getSession();
			final EntityPersister concreteDescriptor = referencedModelPart.getEntityMappingType().getEntityPersister();
			if ( !selectByUniqueKey ) {
				final EntityKey entityKey = new EntityKey( identifier, concreteDescriptor );
				final PersistenceContext persistenceContext = session.getPersistenceContext();

				final LoadingEntityEntry loadingEntityLocally = persistenceContext.getLoadContexts()
						.findLoadingEntityEntry( entityKey );
				if ( loadingEntityLocally != null ) {
					entityInstance = loadingEntityLocally.getEntityInstance();
				}
				if ( entityInstance == null ) {
					entityInstance = persistenceContext.getEntity( entityKey );
					if ( entityInstance != null ) {
						entityInstance = persistenceContext.proxyFor( entityInstance );
					}
				}
			}
			if ( entityInstance == null ) {
				if ( referencedModelPart.isOptional()
						&& parentAccess != null
						&& !parentAccess.isEmbeddableInitializer()
						&& isEnhancedForLazyLoading( parentEntityInitializer ) ) {
					entityInstance = LazyPropertyInitializer.UNFETCHED_PROPERTY;
				}
				else {
					if ( selectByUniqueKey ) {
						final String uniqueKeyPropertyName = referencedModelPart.getReferencedPropertyName();
						final Type uniqueKeyPropertyType = ( referencedModelPart.getReferencedPropertyName() == null ) ?
								concreteDescriptor.getIdentifierType() :
								session.getFactory()
										.getReferencedPropertyType(
												concreteDescriptor.getEntityName(),
												uniqueKeyPropertyName
										);

						final EntityUniqueKey euk = new EntityUniqueKey(
								concreteDescriptor.getEntityName(),
								uniqueKeyPropertyName,
								identifier,
								uniqueKeyPropertyType,
								session.getFactory()
						);
						final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
						entityInstance = persistenceContext.getEntity( euk );
						if ( entityInstance == null ) {
							if ( parentAccess != null
									&& !parentAccess.isEmbeddableInitializer()
									&& isEnhancedForLazyLoading( parentEntityInitializer ) ) {
								return;
							}
							entityInstance = ( (UniqueKeyLoadable) concreteDescriptor ).loadByUniqueKey(
									uniqueKeyPropertyName,
									identifier,
									session
							);

							// If the entity was not in the Persistence Context, but was found now,
							// add it to the Persistence Context
							if ( entityInstance != null ) {
								persistenceContext.addEntity( euk, entityInstance );
							}
						}
						if ( entityInstance != null ) {
							entityInstance = persistenceContext.proxyFor( entityInstance );
						}
					}
					else {
						entityInstance = session.internalLoad(
								concreteDescriptor.getEntityName(),
								identifier,
								false,
								false
						);
					}

					final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( entityInstance );
					if ( lazyInitializer != null ) {
						lazyInitializer.setUnwrap( referencedModelPart.isUnwrapProxy() && concreteDescriptor.isInstrumented() );
					}
				}
			}

			notifyResolutionListeners( entityInstance );
		}
	}

	protected EntityInitializer getParentEntityInitializer(FetchParentAccess parentAccess) {
		if ( parentAccess != null ) {
			return parentAccess.findFirstEntityInitializer();
		}
		return null;
	}

	protected static boolean isEnhancedForLazyLoading(EntityInitializer parentEntityIntialiazer) {
		return parentEntityIntialiazer != null && parentEntityIntialiazer.getEntityDescriptor()
				.getBytecodeEnhancementMetadata()
				.isEnhancedForLazyLoading();
	}

	@Override
	public void initializeInstance(RowProcessingState rowProcessingState) {
		// nothing to do
	}

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {
		entityInstance = null;
		identifier = null;

		clearResolutionListeners();
	}

	@Override
	public EntityPersister getEntityDescriptor() {
		return referencedModelPart.getEntityMappingType().getEntityPersister();
	}

	@Override
	public Object getEntityInstance() {
		return entityInstance;
	}

	protected void setEntityInstance(Object entityInstance) {
		this.entityInstance = entityInstance;
	}

	@Override
	public EntityKey getEntityKey() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isEntityInitialized() {
		return false;
	}

	@Override
	public Object getParentKey() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void registerResolutionListener(Consumer<Object> listener) {
		if ( entityInstance != null ) {
			listener.accept( entityInstance );
		}
		else {
			super.registerResolutionListener( listener );
		}
	}

	@Override
	public FetchParentAccess getFetchParentAccess() {
		return parentAccess;
	}

	@Override
	public EntityPersister getConcreteDescriptor() {
		return getEntityDescriptor();
	}

	@Override
	public String toString() {
		return "EntityDelayedFetchInitializer(" + LoggingHelper.toLoggableString( navigablePath ) + ")";
	}

	protected Object getIdentifier() {
		return identifier;
	}

	protected void setIdentifier(Object identifier) {
		this.identifier = identifier;
	}

	protected boolean isSelectByUniqueKey() {
		return selectByUniqueKey;
	}

	protected DomainResultAssembler<?> getIdentifierAssembler() {
		return identifierAssembler;
	}
}
