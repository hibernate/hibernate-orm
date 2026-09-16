/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.List;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.Generator;
import org.hibernate.generator.internal.CompositeGeneratorBuilder.CompositeGenerator;
import org.hibernate.generator.internal.TenantIdGeneration;
import org.hibernate.id.CompositeNestedGeneratedValueGenerator;
import org.hibernate.id.CompositeNestedGeneratedValueGenerator.GenerationPlan;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.TenantIdMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.ComponentType;
import org.hibernate.type.Type;

import static org.hibernate.generator.EventType.INSERT;

/**
 * Cached tenant attribute paths and identifier generation plans.
 */
public class TenantIdMappingImpl implements TenantIdMapping {
	private final EntityPersister persister;
	private final TenantAttribute attribute;
	private final AttributeMapping tenantAttribute;
	private final TenantIdGeneration identifierGenerator;
	private final List<GenerationPlan> identifierPlans;
	private final ComponentType identifierType;

	public static TenantIdMapping create(EntityPersister persister) {
		final var attribute = findTenantAttribute( persister, persister.getGenerators() );
		final var identifierGenerator = persister.getGenerator() instanceof TenantIdGeneration generator ? generator : null;
		final var plans = persister.getGenerator() instanceof CompositeNestedGeneratedValueGenerator composite
				? composite.getGenerationPlans().stream()
						.filter( plan -> plan.getGenerator() instanceof TenantIdGeneration ).toList()
				: List.<GenerationPlan>of();
		return attribute == null && identifierGenerator == null && plans.isEmpty()
				? null : new TenantIdMappingImpl( persister, attribute, identifierGenerator, plans );
	}

	private TenantIdMappingImpl(
			EntityPersister persister, TenantAttribute attribute,
			TenantIdGeneration identifierGenerator, List<GenerationPlan> identifierPlans) {
		this.persister = persister;
		this.attribute = attribute;
		tenantAttribute = attribute == null ? null : attribute.leafAttribute();
		this.identifierGenerator = identifierGenerator;
		this.identifierPlans = identifierPlans;
		identifierType = identifierPlans.isEmpty() ? null : (ComponentType) persister.getIdentifierType();
	}

	@Override
	public AttributeMapping getAttributeMapping() {
		return tenantAttribute;
	}

	@Override
	public Object getTenantIdFromIdentifier(Object id, SharedSessionContractImplementor session) {
		return identifierGenerator != null ? id
				: identifierPlans.isEmpty() || id == null ? null
				: identifierType.getPropertyValue( id, identifierPlans.get( 0 ).getPropertyIndex(), session );
	}

	@Override
	public boolean hasUnassignedIdentifierTenant(Object id, SharedSessionContractImplementor session) {
		if ( id != null ) {
			for ( var plan : identifierPlans ) {
				if ( identifierType.getPropertyValue( id, plan.getPropertyIndex(), session ) == null ) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void validateIdentifier(Object id, SharedSessionContractImplementor session) {
		if ( !session.isRootTenant() ) {
			if ( identifierGenerator != null ) {
				identifierGenerator.validateTenantId( session, id );
			}
			else if ( id != null ) {
				for ( var plan : identifierPlans ) {
					((TenantIdGeneration) plan.getGenerator()).validateTenantId( session,
							identifierType.getPropertyValue( id, plan.getPropertyIndex(), session ) );
				}
			}
		}
	}

	@Override
	public void validateAssignedValue(Object entity, Object id, SharedSessionContractImplementor session) {
		validateIdentifier( id, session );
		if ( attribute != null ) {
			attribute.validate( entity, session );
		}
	}

	@Override
	public void initializeIdentifier(Object entity, SharedSessionContractImplementor session) {
		if ( identifierGenerator != null ) {
			persister.setIdentifier( entity, identifierGenerator.generate(
					session, entity, persister.getIdentifier( entity, session ), INSERT ), session );
		}
		else if ( !identifierPlans.isEmpty() ) {
			final Object id = persister.getIdentifier( entity, session );
			if ( id != null ) {
				final Object[] values = identifierType.getPropertyValues( id, session );
				for ( var plan : identifierPlans ) {
					final int position = plan.getPropertyIndex();
					values[position] = plan.getGenerator().generate( session, entity, values[position], INSERT );
				}
				persister.setIdentifier( entity, identifierType.replacePropertyValues( id, values, session ), session );
			}
		}
	}

	@Override
	public void initialize(Object entity, Object[] state, SharedSessionContractImplementor session) {
		if ( attribute != null ) {
			final int position = attribute.attribute.getStateArrayPosition();
			state[position] = attribute.generate( state[position], entity, persister.getPropertyTypes()[position], session );
			persister.setValue( entity, position, state[position] );
		}
	}

	private static TenantAttribute findTenantAttribute(ManagedMappingType type, Generator[] generators) {
		for ( int i = 0; i < generators.length; i++ ) {
			final var attribute = type.getAttributeMapping( i );
			if ( generators[i] instanceof TenantIdGeneration generator ) {
				return new TenantAttribute( attribute, generator, null );
			}
			else if ( generators[i] instanceof CompositeGenerator composite ) {
				final var nested = findTenantAttribute(
						attribute.asEmbeddedAttributeMapping().getEmbeddableTypeDescriptor(),
						composite.generators().toArray( Generator[]::new ) );
				if ( nested != null ) {
					return new TenantAttribute( attribute, null, nested );
				}
			}
		}
		return null;
	}

	private record TenantAttribute(AttributeMapping attribute, TenantIdGeneration generator, TenantAttribute nested) {
		AttributeMapping leafAttribute() {
			return nested == null ? attribute : nested.leafAttribute();
		}

		void validate(Object owner, SharedSessionContractImplementor session) {
			final Object value = owner == null ? null : attribute.getValue( owner );
			if ( nested == null ) {
				generator.validateTenantId( session, value );
			}
			else {
				nested.validate( value, session );
			}
		}

		Object generate(Object value, Object entity, Type type, SharedSessionContractImplementor session) {
			if ( nested == null ) {
				return generator.generate( session, entity, value, INSERT );
			}
			final var componentType = (ComponentType) type;
			final Object[] values = value == null
					? new Object[componentType.getPropertyNames().length]
					: componentType.getPropertyValues( value, session );
			final int position = nested.attribute.getStateArrayPosition();
			values[position] = nested.generate( values[position], entity, componentType.getSubtypes()[position], session );
			return value == null
					? attribute.asEmbeddedAttributeMapping().getEmbeddableTypeDescriptor()
						.getRepresentationStrategy().getInstantiator().instantiate( () -> values )
					: componentType.replacePropertyValues( value, values, session );
		}
	}
}
