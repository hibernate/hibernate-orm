/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.AbstractFetchParent;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * AbstractFetchParent sub-class for entity-valued graph nodes
 *
 * @author Steve Ebersole
 */
public abstract class AbstractEntityResultGraphNode extends AbstractFetchParent implements EntityResultGraphNode {
	private final EntityValuedModelPart referencedModelPart;
	private final DomainResult identifierResult;
	private final DomainResult discriminatorResult;
	private final DomainResult versionResult;
	private final LockMode lockMode;

	private final EntityMappingType targetType;

	public AbstractEntityResultGraphNode(
			EntityValuedModelPart referencedModelPart,
			LockMode lockMode,
			NavigablePath navigablePath,
			DomainResultCreationState creationState) {
		this( referencedModelPart, lockMode, navigablePath, null, creationState );
	}

	@SuppressWarnings("WeakerAccess")
	public AbstractEntityResultGraphNode(
			EntityValuedModelPart referencedModelPart,
			LockMode lockMode,
			NavigablePath navigablePath,
			EntityMappingType targetType,
			DomainResultCreationState creationState) {
		super( referencedModelPart.getEntityMappingType(), navigablePath );
		this.referencedModelPart = referencedModelPart;
		this.lockMode = lockMode;
		this.targetType = targetType;

		final EntityMappingType entityDescriptor = referencedModelPart.getEntityMappingType();

		final TableGroup entityTableGroup = creationState.getSqlAstCreationState().getFromClauseAccess().findTableGroup( navigablePath );

		identifierResult = entityDescriptor.getIdentifierMapping().createDomainResult(
				navigablePath.append( EntityIdentifierMapping.ROLE_LOCAL_NAME ),
				entityTableGroup,
				null,
				creationState
		);

		final EntityDiscriminatorMapping discriminatorMapping = getDiscriminatorMapping( entityDescriptor, entityTableGroup );
		if ( discriminatorMapping != null ) {
			discriminatorResult = discriminatorMapping.createDomainResult(
					navigablePath.append( EntityDiscriminatorMapping.ROLE_NAME ),
					entityTableGroup,
					null,
					creationState
			);
		}
		else {
			discriminatorResult = null;
		}

		final EntityVersionMapping versionDescriptor = entityDescriptor.getVersionMapping();
		if ( versionDescriptor == null ) {
			versionResult = null;
		}
		else {
			versionResult = versionDescriptor.createDomainResult(
					navigablePath.append( versionDescriptor.getFetchableName() ),
					entityTableGroup,
					null,
					creationState
			);
		}
	}

	protected EntityDiscriminatorMapping getDiscriminatorMapping(
			EntityMappingType entityDescriptor,
			TableGroup entityTableGroup) {
		return entityDescriptor.getDiscriminatorMapping();
	}

	@Override
	public EntityMappingType getReferencedMappingContainer() {
		return getEntityValuedModelPart().getEntityMappingType();
	}

	@Override
	public EntityValuedModelPart getEntityValuedModelPart() {
		return referencedModelPart;
	}

	@Override
	public JavaTypeDescriptor getResultJavaTypeDescriptor() {
		return getEntityValuedModelPart().getEntityMappingType().getMappedJavaTypeDescriptor();
	}

	public LockMode getLockMode() {
		return lockMode;
	}

	public DomainResult getIdentifierResult() {
		return identifierResult;
	}

	public DomainResult getDiscriminatorResult() {
		return discriminatorResult;
	}

	public DomainResult getVersionResult() {
		return versionResult;
	}
}
