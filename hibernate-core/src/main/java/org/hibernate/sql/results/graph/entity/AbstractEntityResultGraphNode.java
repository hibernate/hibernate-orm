/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity;

import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityRowIdMapping;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.query.EntityIdentifierNavigablePath;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.tree.from.LazyTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.AbstractFetchParent;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

import static org.hibernate.query.results.ResultsHelper.attributeName;

/**
 * AbstractFetchParent sub-class for entity-valued graph nodes
 *
 * @author Steve Ebersole
 */
public abstract class AbstractEntityResultGraphNode extends AbstractFetchParent implements EntityResultGraphNode {
	private final EntityValuedModelPart referencedModelPart;
	private final DomainResult<?> identifierResult;
	private final BasicFetch<?> discriminatorFetch;
	private final DomainResult<Object> rowIdResult;

	private final EntityMappingType targetType;

	public AbstractEntityResultGraphNode(
			EntityValuedModelPart referencedModelPart,
			NavigablePath navigablePath,
			DomainResultCreationState creationState) {
		this( referencedModelPart, navigablePath, null, creationState );
	}

	@SuppressWarnings("WeakerAccess")
	public AbstractEntityResultGraphNode(
			EntityValuedModelPart referencedModelPart,
			NavigablePath navigablePath,
			EntityMappingType targetType,
			DomainResultCreationState creationState) {
		super( referencedModelPart.getEntityMappingType(), navigablePath );
		this.referencedModelPart = referencedModelPart;
		this.targetType = targetType;

		final EntityMappingType entityDescriptor = referencedModelPart.getEntityMappingType();

		final TableGroup entityTableGroup = creationState.getSqlAstCreationState().getFromClauseAccess().findTableGroup( navigablePath );

		final EntityIdentifierMapping identifierMapping = entityDescriptor.getIdentifierMapping();

		final EntityIdentifierNavigablePath identifierNavigablePath = new EntityIdentifierNavigablePath( navigablePath, attributeName( identifierMapping ) );
		if ( navigablePath.getParent() == null && !creationState.forceIdentifierSelection() ) {
			identifierResult = null;
			visitIdentifierMapping( identifierNavigablePath, creationState, identifierMapping, entityTableGroup );
		}
		else if ( referencedModelPart instanceof ToOneAttributeMapping ) {
			// If we don't do this here, LazyTableGroup#getTableReferenceInternal would have to use the target table in case {id} is encountered
			if ( ( (ToOneAttributeMapping) referencedModelPart ).canJoinForeignKey( identifierMapping ) ) {
				identifierResult = ( (ToOneAttributeMapping) referencedModelPart ).getForeignKeyDescriptor()
						.createKeyDomainResult(
								navigablePath,
								creationState.getSqlAstCreationState()
										.getFromClauseAccess()
										.findTableGroup( navigablePath.getParent() ),
								creationState
						);

			}
			else {
				identifierResult = identifierMapping.createDomainResult(
						identifierNavigablePath,
						( (LazyTableGroup) entityTableGroup ).getTableGroup(),
						null,
						creationState
				);
			}
		}
		else {
			identifierResult = identifierMapping.createDomainResult(
					identifierNavigablePath,
					entityTableGroup,
					null,
					creationState
			);
		}

		final EntityDiscriminatorMapping discriminatorMapping = getDiscriminatorMapping( entityDescriptor, entityTableGroup );
		// No need to fetch the discriminator if this type does not have subclasses
		if ( discriminatorMapping != null && entityDescriptor.getEntityPersister().getEntityMetamodel().hasSubclasses() ) {
			discriminatorFetch = discriminatorMapping.generateFetch(
					this,
					navigablePath.append( EntityDiscriminatorMapping.ROLE_NAME ),
					FetchTiming.IMMEDIATE,
					true,
					null,
					creationState
			);
		}
		else {
			discriminatorFetch = null;
		}

		final EntityRowIdMapping rowIdMapping = entityDescriptor.getRowIdMapping();
		if ( rowIdMapping == null ) {
			rowIdResult = null;
		}
		else {
			rowIdResult = rowIdMapping.createDomainResult(
					navigablePath.append( rowIdMapping.getRowIdName() ),
					entityTableGroup,
					AbstractEntityPersister.ROWID_ALIAS,
					creationState
			);
		}
	}

	private void visitIdentifierMapping(
			EntityIdentifierNavigablePath navigablePath,
			DomainResultCreationState creationState,
			EntityIdentifierMapping identifierMapping,
			TableGroup entityTableGroup) {
		final MappingType mappingType = identifierMapping.getPartMappingType();
		if ( mappingType instanceof ManagedMappingType ) {
			( (ManagedMappingType) mappingType ).visitAttributeMappings(
					attributeMapping -> {
						if ( attributeMapping instanceof ToOneAttributeMapping ) {
							( (ToOneAttributeMapping) attributeMapping ).getForeignKeyDescriptor()
									.createKeyDomainResult(
											navigablePath.getParent(),
											entityTableGroup,
											creationState
									);
						}
						else {
							attributeMapping.createDomainResult(
									navigablePath,
									entityTableGroup,
									null,
									creationState
							);
						}
					}
			);
		}
		else {
			identifierMapping.createDomainResult(
					navigablePath,
					entityTableGroup,
					null,
					creationState
			);
		}
	}

	protected EntityDiscriminatorMapping getDiscriminatorMapping(
			EntityMappingType entityDescriptor,
			TableGroup entityTableGroup) {
		return entityDescriptor.getDiscriminatorMapping( entityTableGroup );
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

	public DomainResult getIdentifierResult() {
		return identifierResult;
	}

	public BasicFetch<?> getDiscriminatorFetch() {
		return discriminatorFetch;
	}

	public DomainResult<Object> getRowIdResult() {
		return rowIdResult;
	}
}
