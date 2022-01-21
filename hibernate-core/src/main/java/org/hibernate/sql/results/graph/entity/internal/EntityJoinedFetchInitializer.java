/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import org.hibernate.LockMode;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.entity.AbstractEntityInitializer;
import org.hibernate.sql.results.graph.entity.EntityResultGraphNode;

/**
 * @author Andrea Boriero
 */
public class EntityJoinedFetchInitializer extends AbstractEntityInitializer {
	private static final String CONCRETE_NAME = EntityJoinedFetchInitializer.class.getSimpleName();

	private final boolean isEnhancedForLazyLoading;

	public EntityJoinedFetchInitializer(
			EntityResultGraphNode resultDescriptor,
			ModelPart referencedModelPart,
			NavigablePath navigablePath,
			LockMode lockMode,
			Fetch identifierFetch,
			Fetch discriminatorFetch,
			AssemblerCreationState creationState) {
		super(
				resultDescriptor,
				navigablePath,
				lockMode,
				identifierFetch,
				discriminatorFetch,
				null,
				creationState
		);
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
		ModelPart referencedModelPart = getInitializedPart();
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
}
