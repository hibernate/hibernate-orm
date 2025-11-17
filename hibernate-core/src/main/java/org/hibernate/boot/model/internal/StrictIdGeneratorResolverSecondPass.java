/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.util.UUID;

import org.hibernate.boot.models.annotations.internal.SequenceGeneratorJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.TableGeneratorJpaAnnotation;
import org.hibernate.boot.models.spi.GlobalRegistrations;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.models.spi.MemberDetails;

import jakarta.persistence.GeneratedValue;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.boot.model.internal.GeneratorAnnotationHelper.handleGenericGenerator;
import static org.hibernate.boot.model.internal.GeneratorAnnotationHelper.handleSequenceGenerator;
import static org.hibernate.boot.model.internal.GeneratorAnnotationHelper.handleTableGenerator;
import static org.hibernate.boot.model.internal.GeneratorAnnotationHelper.handleUuidStrategy;

/**
 * SecondPass implementing delayed resolution of id-generators associated with an entity
 * using strict JPA resolution - based mainly on global resolution of generator names,
 * along with support for UUID and String member types with AUTO.  We also account for
 * legacy (un-configurable) named generators ({@code increment}, {@code uuid.hex}, etc.).
 *
 * @implNote For unnamed generators defined on the entity class or on the id member, this
 * strategy will register a global registration using the entity's name and later look it
 * up by that name.  This more strictly follows the JPA specification where all generator
 * names should be considered global and resolved globally.
 *
 * @see IdGeneratorResolverSecondPass
 *
 * @author Steve Ebersole
 */
public class StrictIdGeneratorResolverSecondPass extends AbstractEntityIdGeneratorResolver {
	public StrictIdGeneratorResolverSecondPass(
			PersistentClass entityMapping,
			SimpleValue idValue,
			MemberDetails idMember,
			GeneratedValue generatedValue,
			MetadataBuildingContext buildingContext) {
		super( entityMapping, idValue, idMember, generatedValue, buildingContext );
	}

	private ModelsContext modelsContext() {
		return buildingContext.getBootstrapContext().getModelsContext();
	}

	@Override
	protected void handleUnnamedSequenceGenerator() {
		// according to the spec, this should locate a generator with the same name as the entity-name
		final var globalMatch =
				getGlobalRegistrations().getSequenceGeneratorRegistrations()
						.get( entityMapping.getJpaEntityName() );
		if ( globalMatch != null ) {
			handleSequenceGenerator(
					entityMapping.getJpaEntityName(),
					globalMatch.configuration(),
					idValue,
					idMember,
					buildingContext
			);
		}
		else {
			handleSequenceGenerator(
					entityMapping.getJpaEntityName(),
					new SequenceGeneratorJpaAnnotation( modelsContext() ),
					idValue,
					idMember,
					buildingContext
			);
		}
	}

	@Override
	protected void handleNamedSequenceGenerator() {
		final var globalMatch =
				getGlobalRegistrations().getSequenceGeneratorRegistrations()
						.get( generatedValue.generator() );
		if ( globalMatch != null ) {
			handleSequenceGenerator(
					generatedValue.generator(),
					globalMatch.configuration(),
					idValue,
					idMember,
					buildingContext
			);
		}
		else {
			handleSequenceGenerator(
					generatedValue.generator(),
					new SequenceGeneratorJpaAnnotation( generatedValue.generator(), modelsContext() ),
					idValue,
					idMember,
					buildingContext
			);
		}
	}

	@Override
	protected void handleUnnamedTableGenerator() {
		final var globalMatch =
				getGlobalRegistrations().getTableGeneratorRegistrations()
						.get( entityMapping.getJpaEntityName() );
		if ( globalMatch != null ) {
			handleTableGenerator(
					entityMapping.getJpaEntityName(),
					globalMatch.configuration(),
					idValue,
					idMember,
					buildingContext
			);
		}
		else {
			handleTableGenerator(
					entityMapping.getJpaEntityName(),
					new TableGeneratorJpaAnnotation( modelsContext() ),
					idValue,
					idMember,
					buildingContext
			);
		}
	}

	@Override
	protected void handleNamedTableGenerator() {
		final var globalMatch =
				getGlobalRegistrations().getTableGeneratorRegistrations()
						.get( generatedValue.generator() );
		if ( globalMatch != null ) {
			handleTableGenerator(
					generatedValue.generator(),
					globalMatch.configuration(),
					idValue,
					idMember,
					buildingContext
			);
		}
		else {
			handleTableGenerator(
					generatedValue.generator(),
					new TableGeneratorJpaAnnotation( generatedValue.generator(), modelsContext() ),
					idValue,
					idMember,
					buildingContext
			);
		}
	}

	@Override
	protected void handleUnnamedAutoGenerator() {
		handleAutoGenerator( entityMapping.getJpaEntityName() );
	}

	@Override
	protected void handleNamedAutoGenerator() {
		handleAutoGenerator( generatedValue.generator() );
	}

	private void handleAutoGenerator(String globalRegistrationName) {

		final var globalSequenceMatch =
				getGlobalRegistrations().getSequenceGeneratorRegistrations()
						.get( globalRegistrationName );
		if ( globalSequenceMatch != null ) {
			handleSequenceGenerator(
					globalRegistrationName,
					globalSequenceMatch.configuration(),
					idValue,
					idMember,
					buildingContext
			);
			return;
		}

		final var globalTableMatch =
				getGlobalRegistrations().getTableGeneratorRegistrations()
						.get( globalRegistrationName );
		if ( globalTableMatch != null ) {
			handleTableGenerator(
					globalRegistrationName,
					globalTableMatch.configuration(),
					idValue,
					idMember,
					buildingContext
			);
			return;
		}

		final var globalGenericMatch =
				getGlobalRegistrations().getGenericGeneratorRegistrations()
						.get( globalRegistrationName );
		if ( globalGenericMatch != null ) {
			handleGenericGenerator(
					globalRegistrationName,
					globalGenericMatch.configuration(),
					entityMapping,
					idValue,
					buildingContext
			);
			return;
		}

		if ( handleAsMetaAnnotated() ) {
			return;
		}

		// Implicit handling of UUID generation
		final var idMemberType = idMember.getType();
		if ( idMemberType.isImplementor( UUID.class )
			|| idMemberType.isImplementor( String.class ) ) {
			handleUuidStrategy(
					idValue,
					idMember,
					buildingContext.getMetadataCollector().getClassDetailsRegistry()
							.getClassDetails( entityMapping.getClassName() ),
					buildingContext
			);
			return;
		}

		if ( handleAsLegacyGenerator() ) {
			return;
		}

		handleSequenceGenerator(
				globalRegistrationName,
				new SequenceGeneratorJpaAnnotation( generatedValue.generator(), modelsContext() ),
				idValue,
				idMember,
				buildingContext
		);
	}

	private GlobalRegistrations getGlobalRegistrations() {
		return buildingContext.getMetadataCollector().getGlobalRegistrations();
	}
}
