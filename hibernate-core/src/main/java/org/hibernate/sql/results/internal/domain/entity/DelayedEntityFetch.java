/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.entity;

import java.util.ArrayList;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.metamodel.mapping.internal.SingularAssociationAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.internal.domain.AbstractFetchParentAccess;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.EntityInitializer;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.Fetchable;
import org.hibernate.sql.results.spi.Initializer;
import org.hibernate.sql.results.spi.RowProcessingState;

/**
 * @author Andrea Boriero
 */
public class DelayedEntityFetch implements Fetch {

	private FetchParent fetchParent;
	private SingularAssociationAttributeMapping fetchedAttribute;
	private final LockMode lockMode;
	private final NavigablePath navigablePath;
	private final boolean nullable;
	private final DomainResultCreationState creationState;

	public DelayedEntityFetch(
			FetchParent fetchParent,
			SingularAssociationAttributeMapping fetchedAttribute,
			LockMode lockMode,
			boolean nullable,
			NavigablePath navigablePath,
			DomainResultCreationState creationState) {
		this.fetchParent = fetchParent;
		this.fetchedAttribute = fetchedAttribute;
		this.lockMode = lockMode;
		this.nullable = nullable;
		this.navigablePath = navigablePath;
		this.creationState = creationState;
	}

	@Override
	public FetchParent getFetchParent() {
		return fetchParent;
	}

	@Override
	public Fetchable getFetchedMapping() {
		return fetchedAttribute;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public boolean isNullable() {
		return nullable;
	}

	@Override
	public DomainResultAssembler createAssembler(
			FetchParentAccess parentAccess,
			Consumer<Initializer> collector,
			AssemblerCreationState creationState) {
		EntityInitializer entityInitializer = new DelayedEntityFectInitializer(
				parentAccess,
				navigablePath,
				(EntityPersister) fetchedAttribute.getMappedTypeDescriptor()
		);
		collector.accept( entityInitializer );
		return new EntityAssembler( fetchedAttribute.getJavaTypeDescriptor(), entityInitializer );

	}

	private static class DelayedEntityFectInitializer extends AbstractFetchParentAccess implements EntityInitializer {

		private final FetchParentAccess parentAccess;
		private final NavigablePath navigablePath;
		private final EntityPersister concreteDescriptor;

		private Object entityInstance;

		protected DelayedEntityFectInitializer(
				FetchParentAccess parentAccess,
				NavigablePath fetchedNavigable,
				EntityPersister concreteDescriptor
				) {
			this.parentAccess = parentAccess;
			this.navigablePath = fetchedNavigable;
			this.concreteDescriptor = concreteDescriptor;
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
		public void resolveInstance(RowProcessingState rowProcessingState) {
			final EntityKey entityKey = new EntityKey(
					parentAccess.getParentKey(),
					concreteDescriptor
			);
			Object fkValue = entityKey.getIdentifierValue();

			// todo (6.0) : technically the entity could be managed or cached already.  who/what handles that?

			// todo (6.0) : could also be getting loaded elsewhere (LoadingEntityEntry)
			if ( fkValue == null ) {
				// todo (6.0) : check this is the correct behaviour
				entityInstance = null;
			}
			else {
				if ( concreteDescriptor.hasProxy() ) {
					entityInstance = concreteDescriptor.createProxy(
							fkValue,
							rowProcessingState.getSession()
					);
				}
				else if ( concreteDescriptor
						.getBytecodeEnhancementMetadata()
						.isEnhancedForLazyLoading() ) {
					entityInstance = concreteDescriptor.instantiate(
							fkValue,
							rowProcessingState.getSession()
					);
				}

				notifyParentResolutionListeners( entityInstance );
			}
		}

		@Override
		public void initializeInstance(RowProcessingState rowProcessingState) {
			// nothing to do
		}

		@Override
		public void finishUpRow(RowProcessingState rowProcessingState) {
			entityInstance = null;

			clearParentResolutionListeners();
		}

		@Override
		public EntityPersister getEntityDescriptor() {
			return concreteDescriptor;
		}

		@Override
		public Object getEntityInstance() {
			return entityInstance;
		}

		@Override
		public Object getParentKey() {
			throw new NotYetImplementedFor6Exception( getClass() );
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

	}
}
