/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import org.hibernate.LockMode;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.Status;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.entity.AbstractEntityInitializer;
import org.hibernate.sql.results.graph.entity.EntityResultGraphNode;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

/**
 * @author Andrea Boriero
 */
public class EntityJoinedFetchInitializer extends AbstractEntityInitializer {
	private static final String CONCRETE_NAME = EntityJoinedFetchInitializer.class.getSimpleName();

	private final ModelPart referencedModelPart;
	private final boolean isEnhancedForLazyLoading;

	protected EntityJoinedFetchInitializer(
			EntityResultGraphNode resultDescriptor,
			ModelPart referencedModelPart,
			NavigablePath navigablePath,
			LockMode lockMode,
			DomainResult<?> identifierResult,
			DomainResult<?> discriminatorResult,
			DomainResult<?> versionResult,
			AssemblerCreationState creationState) {
		super(
				resultDescriptor,
				navigablePath,
				lockMode,
				identifierResult,
				discriminatorResult,
				versionResult,
				null,
				creationState
		);
		this.referencedModelPart = referencedModelPart;
		if ( getConcreteDescriptor() != null ) {
			this.isEnhancedForLazyLoading = getConcreteDescriptor().getBytecodeEnhancementMetadata()
					.isEnhancedForLazyLoading();
		}
		else {
			this.isEnhancedForLazyLoading = false;
		}
	}

	@Override
	protected Object getProxy(PersistenceContext persistenceContext) {
		if ( referencedModelPart instanceof ToOneAttributeMapping ) {
			final boolean unwrapProxy = ( (ToOneAttributeMapping) referencedModelPart ).isUnwrapProxy() && isEnhancedForLazyLoading;
			if ( unwrapProxy ) {
				return null;
			}
		}
		return super.getProxy( persistenceContext );
	}

	@Override
	protected String getSimpleConcreteImplName() {
		return CONCRETE_NAME;
	}

	@Override
	protected boolean isEntityReturn() {
		return false;
	}

	@Override
	public String toString() {
		return "EntityJoinedFetchInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}

	@Override
	protected boolean skipInitialization(
			Object toInitialize, RowProcessingState rowProcessingState, EntityEntry entry) {
		if ( toInitialize instanceof PersistentAttributeInterceptable ) {
			final PersistentAttributeInterceptor interceptor = ( (PersistentAttributeInterceptable) toInitialize ).$$_hibernate_getInterceptor();
			if ( interceptor instanceof EnhancementAsProxyLazinessInterceptor ) {
				if ( entry.getStatus() != Status.LOADING ) {
					// Avoid loading the same entity proxy twice for the same result set: it could lead to errors,
					// because some code writes to its input (ID in hydrated state replaced by the loaded entity, in particular).
					return false;
				}
				return true;
			}
		}
		return super.skipInitialization( toInitialize, rowProcessingState, entry );
	}
}
