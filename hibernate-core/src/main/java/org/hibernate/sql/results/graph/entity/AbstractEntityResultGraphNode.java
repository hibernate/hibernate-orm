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
import org.hibernate.spi.EntityIdentifierNavigablePath;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.AbstractFetchParent;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.query.results.ResultsHelper.attributeName;

/**
 * AbstractFetchParent sub-class for entity-valued graph nodes
 *
 * @author Steve Ebersole
 */
public abstract class AbstractEntityResultGraphNode extends AbstractFetchParent implements EntityResultGraphNode {
	private final EntityValuedModelPart referencedModelPart;
	private Fetch identifierFetch;
	private BasicFetch<?> discriminatorFetch;
	private DomainResult<Object> rowIdResult;

	public AbstractEntityResultGraphNode(EntityValuedModelPart referencedModelPart, NavigablePath navigablePath) {
		super( referencedModelPart.getEntityMappingType(), navigablePath );
		this.referencedModelPart = referencedModelPart;
	}

	@Override
	public void afterInitialize(FetchParent fetchParent, DomainResultCreationState creationState) {
		final EntityMappingType entityDescriptor = referencedModelPart.getEntityMappingType();
		final EntityIdentifierMapping identifierMapping = entityDescriptor.getIdentifierMapping();
		final NavigablePath navigablePath = getNavigablePath();
		final TableGroup entityTableGroup = creationState.getSqlAstCreationState().getFromClauseAccess()
				.getTableGroup( navigablePath );
		final EntityIdentifierNavigablePath identifierNavigablePath = new EntityIdentifierNavigablePath( navigablePath, attributeName( identifierMapping ) );
		if ( navigablePath.getParent() == null && !creationState.forceIdentifierSelection() ) {
			identifierFetch = null;
			visitIdentifierMapping( identifierNavigablePath, creationState, identifierMapping, entityTableGroup );
		}
		else {
			identifierFetch = ( (Fetchable) identifierMapping ).generateFetch(
					fetchParent,
					identifierNavigablePath,
					FetchTiming.IMMEDIATE,
					true,
					null,
					creationState
			);
		}

		final EntityDiscriminatorMapping discriminatorMapping = entityDescriptor.getDiscriminatorMapping();
		// No need to fetch the discriminator if this type does not have subclasses
		if ( discriminatorMapping != null && entityDescriptor.hasSubclasses() ) {
			discriminatorFetch = discriminatorMapping.generateFetch(
					fetchParent,
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
		super.afterInitialize( fetchParent, creationState );
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

	@Override
	public EntityMappingType getReferencedMappingContainer() {
		return getEntityValuedModelPart().getEntityMappingType();
	}

	@Override
	public EntityValuedModelPart getEntityValuedModelPart() {
		return referencedModelPart;
	}

	@Override
	public JavaType<?> getResultJavaType() {
		return getEntityValuedModelPart().getEntityMappingType().getMappedJavaType();
	}

	public Fetch getIdentifierFetch() {
		return identifierFetch;
	}

	public BasicFetch<?> getDiscriminatorFetch() {
		return discriminatorFetch;
	}

	public DomainResult<Object> getRowIdResult() {
		return rowIdResult;
	}
}
