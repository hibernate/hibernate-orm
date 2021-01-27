/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.DiscriminatedAssociationModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.StateArrayContributorMetadataAccess;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.type.AnyType;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Singular, any-valued attribute
 *
 * @see org.hibernate.annotations.Any
 *
 * @author Steve Ebersole
 */
public class DiscriminatedAssociationAttributeMapping
		extends AbstractSingularAttributeMapping
		implements DiscriminatedAssociationModelPart {
	private final NavigableRole navigableRole;
	private final DiscriminatedAssociationMapping discriminatorMapping;

	public DiscriminatedAssociationAttributeMapping(
			NavigableRole attributeRole,
			JavaTypeDescriptor<?> baseAssociationJtd,
			ManagedMappingType declaringType,
			int stateArrayPosition,
			StateArrayContributorMetadataAccess attributeMetadataAccess,
			FetchTiming fetchTiming,
			PropertyAccess propertyAccess,
			Property bootProperty,
			AnyType anyType,
			Any bootValueMapping,
			MappingModelCreationProcess creationProcess) {
		super(
				bootProperty.getName(),
				stateArrayPosition,
				attributeMetadataAccess,
				fetchTiming == FetchTiming.IMMEDIATE
						? new FetchStrategy( FetchTiming.IMMEDIATE, FetchStyle.SELECT )
						: new FetchStrategy( FetchTiming.DELAYED, FetchStyle.SELECT ),
				declaringType,
				propertyAccess
		);
		this.navigableRole = attributeRole;

		this.discriminatorMapping = DiscriminatedAssociationMapping.from(
				attributeRole,
				baseAssociationJtd,
				this,
				anyType,
				bootValueMapping,
				creationProcess
		);
	}

	@Override
	public BasicValuedModelPart getDiscriminatorPart() {
		return discriminatorMapping.getDiscriminatorPart();
	}

	@Override
	public BasicValuedModelPart getKeyPart() {
		return discriminatorMapping.getKeyPart();
	}

	@Override
	public EntityMappingType resolveDiscriminatorValue(Object discriminatorValue) {
		return discriminatorMapping.resolveDiscriminatorValueToEntityName( discriminatorValue );
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			LockMode lockMode,
			String resultVariable,
			DomainResultCreationState creationState) {
		return discriminatorMapping.generateFetch(
				fetchParent,
				fetchablePath,
				fetchTiming,
				selected,
				lockMode,
				resultVariable,
				creationState
		);
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public MappingType getMappedType() {
		return discriminatorMapping;
	}

	@Override
	public int getNumberOfFetchables() {
		return 2;
	}

	@Override
	public int getJdbcTypeCount() {
		return getDiscriminatorPart().getJdbcTypeCount() + getKeyPart().getJdbcTypeCount();
	}

	@Override
	public void breakDownJdbcValues(Object domainValue, JdbcValueConsumer valueConsumer, SharedSessionContractImplementor session) {
		discriminatorMapping.getDiscriminatorPart().breakDownJdbcValues( domainValue, valueConsumer, session );
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		int span = getDiscriminatorPart().forEachJdbcType( offset, action );
		return span + getKeyPart().forEachJdbcType( offset + span, action );
	}

	@Override
	public void visitFetchables(Consumer<Fetchable> fetchableConsumer, EntityMappingType treatTargetType) {
		fetchableConsumer.accept( getDiscriminatorPart() );
		fetchableConsumer.accept( getKeyPart() );
	}

	@Override
	public ModelPart findSubPart(String name, EntityMappingType treatTargetType) {
		if ( AnyDiscriminatorPart.ROLE_NAME.equals( name ) ) {
			return getDiscriminatorPart();
		}

		if ( AnyKeyPart.ROLE_NAME.equals( name ) ) {
			return getKeyPart();
		}

		return null;
	}

	@Override
	public void visitSubParts(Consumer<ModelPart> consumer, EntityMappingType treatTargetType) {
		consumer.accept( getDiscriminatorPart() );
		consumer.accept( getKeyPart() );
	}
}
