/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import org.hibernate.metamodel.mapping.EntityAssociationMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Selects just the FK and builds a proxy
 *
 * @author Christian Beikov
 */
public class EntityDelayedResultImpl implements DomainResult {

	private final NavigablePath navigablePath;
	private final EntityAssociationMapping entityValuedModelPart;
	private final DomainResult identifierResult;

	public EntityDelayedResultImpl(
			NavigablePath navigablePath,
			EntityAssociationMapping entityValuedModelPart,
			TableGroup rootTableGroup,
			DomainResultCreationState creationState) {
		this.navigablePath = navigablePath;
		this.entityValuedModelPart = entityValuedModelPart;
		this.identifierResult = entityValuedModelPart.getForeignKeyDescriptor().createKeyDomainResult(
				navigablePath.append( EntityIdentifierMapping.ROLE_LOCAL_NAME ),
				rootTableGroup,
				creationState
		);
	}

	@Override
	public JavaType<?> getResultJavaType() {
		return entityValuedModelPart.getAssociatedEntityMappingType().getMappedJavaType();
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public String getResultVariable() {
		return null;
	}

	@Override
	public DomainResultAssembler createResultAssembler(
			FetchParentAccess parentAccess,
			AssemblerCreationState creationState) {
		final EntityInitializer initializer = (EntityInitializer) creationState.resolveInitializer(
				getNavigablePath(),
				entityValuedModelPart,
				() -> new EntityDelayedFetchInitializer(
						null,
						getNavigablePath(),
						(ToOneAttributeMapping) entityValuedModelPart,
						false,
						identifierResult.createResultAssembler( parentAccess, creationState )
				)
		);

		return new EntityAssembler( getResultJavaType(), initializer );
	}

	@Override
	public String toString() {
		return "EntityDelayedResultImpl {" + getNavigablePath() + "}";
	}
}
