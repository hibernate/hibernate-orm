/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.entity;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.internal.domain.AbstractFetchParent;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.EntityMappingNode;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractEntityMappingNode extends AbstractFetchParent implements EntityMappingNode {
	private final EntityValuedModelPart referencedModelPart;
	private final DomainResult identifierResult;
	private final DomainResult discriminatorResult;
	private final DomainResult versionResult;
	private final LockMode lockMode;

	private final EntityMappingType targetType;

	private final List<DomainResult> attributeDomainResults = new ArrayList<>();

	public AbstractEntityMappingNode(
			EntityValuedModelPart referencedModelPart,
			LockMode lockMode,
			NavigablePath navigablePath,
			DomainResultCreationState creationState) {
		this( referencedModelPart, lockMode, navigablePath, null, creationState );
	}

	public AbstractEntityMappingNode(
			EntityValuedModelPart referencedModelPart,
			LockMode lockMode,
			NavigablePath navigablePath,
			EntityMappingType targetType,
			DomainResultCreationState creationState) {
		super( referencedModelPart, navigablePath );
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

//		final DiscriminatorMappDescriptor<?> discriminatorDescriptor = entityDescriptor.getHierarchy().getDiscriminatorDescriptor();
//		if ( discriminatorDescriptor == null ) {
//			discriminatorResult = null;
//		}
//		else {
//			discriminatorResult = discriminatorDescriptor.createDomainResult(
//					navigablePath.append( DiscriminatorDescriptor.NAVIGABLE_NAME ),
//					null,
//					creationState
//			);
//		}
		discriminatorResult = null;

		final EntityVersionMapping versionDescriptor = entityDescriptor.getVersionMapping();
		if ( versionDescriptor == null ) {
			versionResult = null;
		}
		else {
			versionResult = versionDescriptor.createDomainResult(
					navigablePath.append( versionDescriptor.getAttributeName() ),
					entityTableGroup,
					null,
					creationState
			);
		}

		entityDescriptor.visitAttributeMappings(
				mapping -> attributeDomainResults.add(
						mapping.createDomainResult(
								navigablePath.append( mapping.getAttributeName() ),
								entityTableGroup,
								null,
								creationState
						)
				)
		);

		// todo (6.0) : handle other special navigables such as discriminator, row-id, tenant-id, etc
	}

	@Override
	public EntityValuedModelPart getReferencedMappingContainer() {
		return getEntityValuedModelPart();
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

	protected DomainResult getIdentifierResult() {
		return identifierResult;
	}

	protected DomainResult getDiscriminatorResult() {
		return discriminatorResult;
	}

	protected DomainResult getVersionResult() {
		return versionResult;
	}
}
