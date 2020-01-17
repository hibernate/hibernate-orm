/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.Collection;
import java.util.List;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.Component;
import org.hibernate.metamodel.mapping.CompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.type.CompositeType;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * A "non-aggregated" composite identifier.
 *
 * This is an identifier mapped using JPA's {@link javax.persistence.MapsId} feature.
 *
 * @apiNote Technically a MapsId id does not have to be composite; we still handle that this class however
 *
 * @author Steve Ebersole
 */
public class NonAggregatedIdentifierMappingImpl implements CompositeIdentifierMapping {
	private final NavigableRole navigableRole;
	private final EntityMappingType entityMapping;

	private final List<SingularAttributeMapping> idAttributeMappings;

	public NonAggregatedIdentifierMappingImpl(
			EntityMappingType entityMapping,
			List<SingularAttributeMapping> idAttributeMappings,
			Component bootIdDescriptor,
			CompositeType cidType,
			MappingModelCreationProcess creationProcess) {
		// todo (6.0) : handle MapsId and IdClass
		// todo (6.0) : implement SQL AST apis (DomainResult, e.g.)
		this.navigableRole = entityMapping.getNavigableRole().appendContainer( EntityIdentifierMapping.ROLE_LOCAL_NAME );
		this.entityMapping = entityMapping;
		this.idAttributeMappings = idAttributeMappings;
	}

	@Override
	public EntityMappingType getPartMappingType() {
		return entityMapping;
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return entityMapping.getJavaTypeDescriptor();
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public EntityMappingType getMappedTypeDescriptor() {
		return entityMapping;
	}

	@Override
	public int getAttributeCount() {
		return idAttributeMappings.size();
	}

	@Override
	public Collection<SingularAttributeMapping> getAttributes() {
		return idAttributeMappings;
	}

	@Override
	public Object getIdentifier(Object entity, SharedSessionContractImplementor session) {
		return entity;
	}

	@Override
	public void setIdentifier(Object entity, Object id, SharedSessionContractImplementor session) {
		// nothing to do
	}

	@Override
	public Object instantiate() {
		return entityMapping;
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		// we will need a specialized impl for this
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return entityMapping;
	}

}
