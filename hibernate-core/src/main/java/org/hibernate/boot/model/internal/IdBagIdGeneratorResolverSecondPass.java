/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.SequenceGeneratorJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.TableGeneratorJpaAnnotation;
import org.hibernate.boot.models.spi.GlobalRegistrations;
import org.hibernate.boot.models.spi.SequenceGeneratorRegistration;
import org.hibernate.boot.models.spi.TableGeneratorRegistration;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.generator.Generator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.mapping.IdentifierBag;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.resource.beans.container.spi.BeanContainer;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;

import static org.hibernate.boot.model.internal.GeneratorBinder.createGeneratorFrom;
import static org.hibernate.boot.model.internal.GeneratorBinder.makeIdGenerator;
import static org.hibernate.boot.model.internal.GeneratorParameters.fallbackAllocationSize;
import static org.hibernate.id.IdentifierGenerator.GENERATOR_NAME;
import static org.hibernate.id.OptimizableGenerator.INCREMENT_PARAM;

/**
 * IdGeneratorResolver for handling generators assigned to id-bag mappings
 *
 * @author Andrea Boriero
 */
public class IdBagIdGeneratorResolverSecondPass implements IdGeneratorResolver {
	private final PersistentClass entityMapping;
	private final SimpleValue idValue;
	private final MemberDetails idBagMember;
	private final String generatorType;
	private final String generatorName;
	private final MetadataBuildingContext buildingContext;
	private final Map<String,String> configuration;

	public IdBagIdGeneratorResolverSecondPass(
			IdentifierBag idBagMapping,
			SimpleValue idValue,
			MemberDetails idBagMember,
			String generatorType,
			String generatorName,
			MetadataBuildingContext buildingContext) {
		this.entityMapping = null;
		this.idValue = idValue;
		this.idBagMember = idBagMember;
		this.generatorType = generatorType;
		this.generatorName = generatorName;
		this.buildingContext = buildingContext;

		this.configuration = new HashMap<>();
	}

	@Override
	public void doSecondPass(Map<String, PersistentClass> idGeneratorDefinitionMap) throws MappingException {
		final GeneratedValue generatedValue = idBagMember.getDirectAnnotationUsage( GeneratedValue.class );
		switch ( generatedValue.strategy() ) {
			case UUID -> GeneratorAnnotationHelper.handleUuidStrategy( idValue, idBagMember, buildingContext );
			case IDENTITY -> GeneratorAnnotationHelper.handleIdentityStrategy( idValue );
			case SEQUENCE -> handleSequenceStrategy(
					generatorName,
					idValue,
					idBagMember,
					buildingContext
			);
			case TABLE -> handleTableStrategy(
					generatorName,
					entityMapping,
					idValue,
					idBagMember,
					buildingContext
			);
			case AUTO -> handleAutoStrategy(
					generatorName,
					idValue,
					idBagMember,
					buildingContext
			);
		}
	}

	private void handleTableStrategy(
			String generatorName,
			PersistentClass entityMapping,
			SimpleValue idValue,
			MemberDetails idBagMember,
			MetadataBuildingContext buildingContext) {
		final InFlightMetadataCollector metadataCollector = buildingContext.getMetadataCollector();
		final GlobalRegistrations globalRegistrations = metadataCollector.getGlobalRegistrations();

		final TableGeneratorRegistration globalTableGenerator =
				globalRegistrations.getTableGeneratorRegistrations().get( generatorName );
		if ( globalTableGenerator != null ) {
			handleTableGenerator(
					generatorName,
					globalTableGenerator.configuration(),
					idValue,
					idBagMember,
					buildingContext
			);
			return;
		}

		final TableGenerator localizedTableMatch = GeneratorAnnotationHelper.findLocalizedMatch(
				JpaAnnotations.TABLE_GENERATOR,
				idBagMember,
				TableGenerator::name,
				generatorName,
				buildingContext
		);
		if ( localizedTableMatch != null ) {
			handleTableGenerator( generatorName, localizedTableMatch, idValue, idBagMember, buildingContext );
			return;
		}

		GeneratorAnnotationHelper.handleTableGenerator(
				generatorName,
				new TableGeneratorJpaAnnotation( metadataCollector.getSourceModelBuildingContext() ),
				entityMapping,
				idValue,
				idBagMember,
				buildingContext
		);
	}

	private void handleSequenceStrategy(
			String generatorName,
			SimpleValue idValue,
			MemberDetails idBagMember,
			MetadataBuildingContext buildingContext) {
		final InFlightMetadataCollector metadataCollector = buildingContext.getMetadataCollector();
		final GlobalRegistrations globalRegistrations = metadataCollector.getGlobalRegistrations();

		final SequenceGeneratorRegistration globalSequenceGenerator =
				globalRegistrations.getSequenceGeneratorRegistrations().get( generatorName );
		if ( globalSequenceGenerator != null ) {
			handleSequenceGenerator(
					generatorName,
					globalSequenceGenerator.configuration(),
					idValue,
					idBagMember,
					buildingContext
			);
			return;
		}

		final SequenceGenerator localizedSequencedMatch = GeneratorAnnotationHelper.findLocalizedMatch(
				JpaAnnotations.SEQUENCE_GENERATOR,
				idBagMember,
				SequenceGenerator::name,
				generatorName,
				buildingContext
		);
		if ( localizedSequencedMatch != null ) {
			handleSequenceGenerator( generatorName, localizedSequencedMatch, idValue, idBagMember, buildingContext );
			return;
		}

		handleSequenceGenerator(
				generatorName,
				new SequenceGeneratorJpaAnnotation( metadataCollector.getSourceModelBuildingContext() ),
				idValue,
				idBagMember,
				buildingContext
		);
	}

	private void handleAutoStrategy(
			String generatorName,
			SimpleValue idValue,
			MemberDetails idBagMember,
			MetadataBuildingContext buildingContext) {
		final GlobalRegistrations globalRegistrations =
				buildingContext.getMetadataCollector().getGlobalRegistrations();

		final SequenceGeneratorRegistration globalSequenceGenerator =
				globalRegistrations.getSequenceGeneratorRegistrations().get( generatorName );
		if ( globalSequenceGenerator != null ) {
			handleSequenceGenerator(
					generatorName,
					globalSequenceGenerator.configuration(),
					idValue,
					idBagMember,
					buildingContext
			);
			return;
		}

		final TableGeneratorRegistration globalTableGenerator =
				globalRegistrations.getTableGeneratorRegistrations().get( generatorName );
		if ( globalTableGenerator != null ) {
			handleTableGenerator(
					generatorName,
					globalTableGenerator.configuration(),
					idValue,
					idBagMember,
					buildingContext
			);
			return;
		}


		final Class<? extends Generator> legacyNamedGenerator =
				GeneratorStrategies.mapLegacyNamedGenerator( generatorName, idValue );
		if ( legacyNamedGenerator != null ) {
			//generator settings
			if ( idValue.getColumnSpan() == 1 ) {
				configuration.put( PersistentIdentifierGenerator.PK, idValue.getColumns().get(0).getName() );
			}
			createGeneratorFrom(
					new IdentifierGeneratorDefinition( generatorName, legacyNamedGenerator.getName(), configuration ),
					idValue,
					(Map) configuration,
					buildingContext
			);
			return;
		}

		final SequenceGenerator localizedSequencedMatch = GeneratorAnnotationHelper.findLocalizedMatch(
				JpaAnnotations.SEQUENCE_GENERATOR,
				idBagMember,
				SequenceGenerator::name,
				generatorName,
				buildingContext
		);
		if ( localizedSequencedMatch != null ) {
			handleSequenceGenerator( generatorName, localizedSequencedMatch, idValue, idBagMember, buildingContext );
			return;
		}

		final TableGenerator localizedTableMatch = GeneratorAnnotationHelper.findLocalizedMatch(
				JpaAnnotations.TABLE_GENERATOR,
				idBagMember,
				TableGenerator::name,
				generatorName,
				buildingContext
		);
		if ( localizedTableMatch != null ) {
			handleTableGenerator( generatorName, localizedTableMatch, idValue, idBagMember, buildingContext );
			return;
		}

		makeIdGenerator( idValue, idBagMember, generatorType, generatorName, buildingContext, null );
	}

	public static void handleSequenceGenerator(
			String nameFromGeneratedValue,
			SequenceGenerator generatorAnnotation,
			SimpleValue idValue,
			MemberDetails idBagMember,
			MetadataBuildingContext buildingContext) {
		idValue.setCustomIdGeneratorCreator( (creationContext) -> {
			final BeanContainer beanContainer = GeneratorBinder.beanContainer( buildingContext );
			final SequenceStyleGenerator identifierGenerator = GeneratorBinder.instantiateGenerator(
					beanContainer,
					SequenceStyleGenerator.class
			);
			GeneratorAnnotationHelper.prepareForUse(
					identifierGenerator,
					generatorAnnotation,
					idBagMember,
					properties -> {
						if ( generatorAnnotation != null ) {
							properties.put( GENERATOR_NAME, generatorAnnotation.name() );
						}
						else if ( nameFromGeneratedValue != null ) {
							properties.put( GENERATOR_NAME, nameFromGeneratedValue );
						}
						// we need to better handle default allocation-size here...
						properties.put( INCREMENT_PARAM, fallbackAllocationSize( generatorAnnotation, buildingContext ) );
					},
					generatorAnnotation == null
							? null
							: (a, properties) -> SequenceStyleGenerator.applyConfiguration( generatorAnnotation, properties::put ),
					creationContext
			);
			return identifierGenerator;
		} );
	}

	public static void handleTableGenerator(
			String nameFromGeneratedValue,
			TableGenerator generatorAnnotation,
			SimpleValue idValue,
			MemberDetails idBagMember,
			MetadataBuildingContext buildingContext) {
		idValue.setCustomIdGeneratorCreator( (creationContext) -> {
			final BeanContainer beanContainer = GeneratorBinder.beanContainer( buildingContext );
			final org.hibernate.id.enhanced.TableGenerator identifierGenerator = GeneratorBinder.instantiateGenerator(
					beanContainer,
					org.hibernate.id.enhanced.TableGenerator.class
			);
			GeneratorAnnotationHelper.prepareForUse(
					identifierGenerator,
					generatorAnnotation,
					idBagMember,
					properties -> {
						if ( generatorAnnotation != null ) {
							properties.put( GENERATOR_NAME, generatorAnnotation.name() );
						}
						else if ( nameFromGeneratedValue != null ) {
							properties.put( GENERATOR_NAME, nameFromGeneratedValue );
						}
						// we need to better handle default allocation-size here...
						properties.put( INCREMENT_PARAM, fallbackAllocationSize( generatorAnnotation, buildingContext ) );
					},
					generatorAnnotation == null
							? null
							: (a, properties) -> org.hibernate.id.enhanced.TableGenerator.applyConfiguration( generatorAnnotation, properties::put ),
					creationContext
			);
			return identifierGenerator;
		} );
	}
}
