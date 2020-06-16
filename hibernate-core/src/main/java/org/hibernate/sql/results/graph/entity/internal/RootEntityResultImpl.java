/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.util.ArrayList;

import org.hibernate.LockMode;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.internal.SingleAttributeIdentifierMapping;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.AbstractFetchParent;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchableContainer;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.graph.entity.EntityResult;
import org.hibernate.sql.results.graph.entity.EntityResultGraphNode;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Andrea Boriero
 */
public class RootEntityResultImpl extends AbstractFetchParent implements EntityResultGraphNode, EntityResult {

	private final String resultVariable;

	private final EntityValuedModelPart referencedModelPart;
	private final DomainResult discriminatorResult;
	private final DomainResult versionResult;
	private DomainResult identifierResult;
	private final LockMode lockMode;

	public RootEntityResultImpl(
			NavigablePath navigablePath,
			EntityValuedModelPart entityValuedModelPart,
			String resultVariable,
			DomainResultCreationState creationState) {
		this( navigablePath, entityValuedModelPart, resultVariable, null, creationState );
	}

	@SuppressWarnings("WeakerAccess")
	public RootEntityResultImpl(
			NavigablePath navigablePath,
			EntityValuedModelPart entityValuedModelPart,
			String resultVariable,
			EntityMappingType targetType,
			DomainResultCreationState creationState) {
		super( entityValuedModelPart.getEntityMappingType(), navigablePath );
		this.resultVariable = resultVariable;
		this.referencedModelPart = entityValuedModelPart;
		this.lockMode = creationState.getSqlAstCreationState().determineLockMode( resultVariable );

		final EntityMappingType entityDescriptor = referencedModelPart.getEntityMappingType();

		final TableGroup entityTableGroup = creationState.getSqlAstCreationState().getFromClauseAccess().findTableGroup( navigablePath );

		EntityIdentifierMapping identifierMapping = entityDescriptor.getIdentifierMapping();
		if ( identifierMapping instanceof SingleAttributeIdentifierMapping ) {
			identifierMapping.createDomainResult(
					navigablePath.append( EntityIdentifierMapping.ROLE_LOCAL_NAME ),
					entityTableGroup,
					null,
					creationState
			);
		}
		else {
			visitCompositeIdentifierMapping( navigablePath, creationState, identifierMapping, entityTableGroup );
		}

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

		afterInitialize( creationState );
	}

	private void visitCompositeIdentifierMapping(
			NavigablePath navigablePath,
			DomainResultCreationState creationState,
			EntityIdentifierMapping identifierMapping,
			TableGroup entityTableGroup) {
		ManagedMappingType mappingType = (ManagedMappingType) identifierMapping.getPartMappingType();
		fetches = new ArrayList<>();
		mappingType.visitAttributeMappings(
				attributeMapping -> {
					if ( attributeMapping instanceof ToOneAttributeMapping ) {
						((ToOneAttributeMapping)attributeMapping).getForeignKeyDescriptor().createDomainResult(
								navigablePath.append( EntityIdentifierMapping.ROLE_LOCAL_NAME ),
								entityTableGroup,
								null,
								creationState
						);
					}
					else {
						attributeMapping.createDomainResult(
								navigablePath.append( EntityIdentifierMapping.ROLE_LOCAL_NAME ),
								entityTableGroup,
								null,
								creationState
						);
					}
				}
		);
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

	public DomainResult getDiscriminatorResult() {
		return discriminatorResult;
	}

	public DomainResult getVersionResult() {
		return versionResult;
	}

	@Override
	public FetchableContainer getReferencedMappingType() {
		return getReferencedMappingContainer();
	}

	@Override
	public EntityValuedModelPart getReferencedModePart() {
		return getEntityValuedModelPart();
	}

	@Override
	public String getResultVariable() {
		return resultVariable;
	}

	@Override
	public DomainResultAssembler createResultAssembler(AssemblerCreationState creationState) {
		// todo (6.0) : seems like here is where we ought to determine the SQL selection mappings

		final EntityInitializer initializer = (EntityInitializer) creationState.resolveInitializer(
				getNavigablePath(),
				() -> new EntityResultInitializer(
						this,
						getNavigablePath(),
						getLockMode(),
						identifierResult,
						getDiscriminatorResult(),
						getVersionResult(),
						creationState
				)
		);

		return new EntityAssembler( getResultJavaTypeDescriptor(), initializer );
	}

	@Override
	public String toString() {
		return "EntityResultImpl {" + getNavigablePath() + "}";
	}
}
