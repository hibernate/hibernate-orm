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
import org.hibernate.mapping.IdentifierBag;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.models.spi.MemberDetails;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;

import static org.hibernate.boot.model.internal.GeneratorAnnotationHelper.findLocalizedMatch;
import static org.hibernate.boot.model.internal.GeneratorAnnotationHelper.handleIdentityStrategy;
import static org.hibernate.boot.model.internal.GeneratorAnnotationHelper.handleSequenceGenerator;
import static org.hibernate.boot.model.internal.GeneratorAnnotationHelper.handleTableGenerator;
import static org.hibernate.boot.model.internal.GeneratorAnnotationHelper.handleUuidStrategy;
import static org.hibernate.boot.model.internal.GeneratorBinder.createGeneratorFrom;
import static org.hibernate.boot.model.internal.GeneratorBinder.makeIdGenerator;
import static org.hibernate.boot.model.internal.GeneratorStrategies.mapLegacyNamedGenerator;

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
			case UUID -> handleUuidStrategy(
					idValue,
					idBagMember,
					buildingContext.getMetadataCollector().getClassDetailsRegistry()
							.getClassDetails( entityMapping.getClassName() ),
					buildingContext
			);
			case IDENTITY -> handleIdentityStrategy( idValue );
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

		final TableGeneratorRegistration globalTableGenerator =
				metadataCollector.getGlobalRegistrations()
						.getTableGeneratorRegistrations()
						.get( generatorName );
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

		final TableGenerator localizedTableMatch = findLocalizedMatch(
				JpaAnnotations.TABLE_GENERATOR,
				idBagMember,
				buildingContext.getMetadataCollector().getClassDetailsRegistry()
						.getClassDetails( entityMapping.getClassName() ),
				TableGenerator::name,
				generatorName,
				buildingContext
		);
		if ( localizedTableMatch != null ) {
			handleTableGenerator( generatorName, localizedTableMatch, idValue, idBagMember, buildingContext );
			return;
		}

		handleTableGenerator(
				generatorName,
				new TableGeneratorJpaAnnotation( metadataCollector.getSourceModelBuildingContext() ),
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

		final SequenceGeneratorRegistration globalSequenceGenerator =
				metadataCollector.getGlobalRegistrations()
						.getSequenceGeneratorRegistrations()
						.get( generatorName );
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

		final SequenceGenerator localizedSequencedMatch = findLocalizedMatch(
				JpaAnnotations.SEQUENCE_GENERATOR,
				idBagMember,
				buildingContext.getMetadataCollector().getClassDetailsRegistry()
						.getClassDetails( entityMapping.getClassName() ),
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


		final Class<? extends Generator> legacyNamedGenerator = mapLegacyNamedGenerator( generatorName, idValue );
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

		final SequenceGenerator localizedSequencedMatch = findLocalizedMatch(
				JpaAnnotations.SEQUENCE_GENERATOR,
				idBagMember,
				buildingContext.getMetadataCollector().getClassDetailsRegistry()
						.getClassDetails( entityMapping.getClassName() ),
				SequenceGenerator::name,
				generatorName,
				buildingContext
		);
		if ( localizedSequencedMatch != null ) {
			handleSequenceGenerator( generatorName, localizedSequencedMatch, idValue, idBagMember, buildingContext );
			return;
		}

		final TableGenerator localizedTableMatch = findLocalizedMatch(
				JpaAnnotations.TABLE_GENERATOR,
				idBagMember,
				buildingContext.getMetadataCollector().getClassDetailsRegistry()
						.getClassDetails( entityMapping.getClassName() ),
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
}
