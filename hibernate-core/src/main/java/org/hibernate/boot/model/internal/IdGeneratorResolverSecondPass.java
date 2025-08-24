/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.util.Locale;
import java.util.UUID;

import org.hibernate.MappingException;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.GenericGeneratorAnnotation;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.models.spi.MemberDetails;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;

import static org.hibernate.boot.model.internal.GeneratorAnnotationHelper.findLocalizedMatch;
import static org.hibernate.boot.model.internal.GeneratorAnnotationHelper.handleSequenceGenerator;
import static org.hibernate.boot.model.internal.GeneratorStrategies.mapLegacyNamedGenerator;

/**
 * SecondPass implementing delayed resolution of id-generators associated with an entity.
 *
 * @see StrictIdGeneratorResolverSecondPass
 *
 * @author Steve Ebersole
 */
public class IdGeneratorResolverSecondPass extends AbstractEntityIdGeneratorResolver {
	public IdGeneratorResolverSecondPass(
			PersistentClass entityMapping,
			SimpleValue idValue,
			MemberDetails idMember,
			GeneratedValue generatedValue,
			MetadataBuildingContext buildingContext) {
		super( entityMapping, idValue, idMember, generatedValue, buildingContext );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SEQUENCE

	@Override
	protected void handleUnnamedSequenceGenerator() {
		// todo (7.0) : null or entityMapping.getJpaEntityName() for "name from GeneratedValue"?

		final var localizedMatch = findLocalizedMatch(
				JpaAnnotations.SEQUENCE_GENERATOR,
				idMember,
				buildingContext.getMetadataCollector().getClassDetailsRegistry()
						.getClassDetails( entityMapping.getClassName() ),
				null,
				null,
				buildingContext
		);
		if ( localizedMatch != null ) {
			handleSequenceGenerator( null, localizedMatch, idValue, idMember, buildingContext );
		}
		else {
			handleSequenceGenerator( null, null, idValue, idMember, buildingContext );
		}
	}

	@Override
	protected void handleNamedSequenceGenerator() {
		final String generator = generatedValue.generator();
		final var collector = buildingContext.getMetadataCollector();
		final var localizedMatch = findLocalizedMatch(
				JpaAnnotations.SEQUENCE_GENERATOR,
				idMember,
				collector.getClassDetailsRegistry()
						.getClassDetails( entityMapping.getClassName() ),
				SequenceGenerator::name,
				generator,
				buildingContext
		);
		if ( localizedMatch != null ) {
			handleSequenceGenerator( generator, localizedMatch, idValue, idMember, buildingContext );
		}
		else {
			// look for the matching global registration, if one.
			final var globalMatch =
					collector.getGlobalRegistrations()
							.getSequenceGeneratorRegistrations()
							.get( generator );
			if ( globalMatch != null ) {
				handleSequenceGenerator( generator, globalMatch.configuration(), idValue, idMember, buildingContext );
			}
			else {
				validateSequenceGeneration();
				handleSequenceGenerator( generator, null, idValue, idMember, buildingContext );
			}
		}
	}

	private void validateSequenceGeneration() {
		// basically, make sure there is neither a TableGenerator nor GenericGenerator with this name

		final var globalRegistrations = buildingContext.getMetadataCollector().getGlobalRegistrations();
		final String generator = generatedValue.generator();

		final var globalTableMatch = globalRegistrations.getTableGeneratorRegistrations().get( generator );
		if ( globalTableMatch != null ) {
			throw new MappingException(
					String.format(
							Locale.ROOT,
							"@GeneratedValue for %s (%s) specified SEQUENCE generation, but referred to a @TableGenerator",
							entityMapping.getEntityName(),
							generator
					)
			);
		}

		final var globalGenericMatch = globalRegistrations.getGenericGeneratorRegistrations().get( generator );
		if ( globalGenericMatch != null ) {
			throw new MappingException(
					String.format(
							Locale.ROOT,
							"@GeneratedValue for %s (%s) specified SEQUENCE generation, but referred to a @GenericGenerator",
							entityMapping.getEntityName(),
							generator
					)
			);
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// TABLE

	@Override
	protected void handleUnnamedTableGenerator() {
		// todo (7.0) : null or entityMapping.getJpaEntityName() for "name from GeneratedValue"?

		final var localizedMatch = findLocalizedMatch(
				JpaAnnotations.TABLE_GENERATOR,
				idMember,
				buildingContext.getMetadataCollector().getClassDetailsRegistry()
						.getClassDetails( entityMapping.getClassName() ),
				null,
				null,
				buildingContext
		);
		handleTableGenerator( null, localizedMatch );
	}

	@Override
	protected void handleNamedTableGenerator() {
		final String generator = generatedValue.generator();
		final var collector = buildingContext.getMetadataCollector();
		final var localizedTableMatch = findLocalizedMatch(
				JpaAnnotations.TABLE_GENERATOR,
				idMember,
				collector.getClassDetailsRegistry()
						.getClassDetails( entityMapping.getClassName() ),
				TableGenerator::name,
				generator,
				buildingContext
		);
		if ( localizedTableMatch != null ) {
			handleTableGenerator( generator, localizedTableMatch );
		}
		else {
			// look for the matching global registration, if one.
			final var globalMatch =
					collector.getGlobalRegistrations()
							.getTableGeneratorRegistrations().get( generator );
			if ( globalMatch != null ) {
				handleTableGenerator( generator, globalMatch.configuration() );
			}
			else {
				validateTableGeneration();
				handleTableGenerator( generator, null );
			}
		}
	}

	private void validateTableGeneration() {
		// basically, make sure there is neither a SequenceGenerator nor a GenericGenerator with this name

		final var globalRegistrations = buildingContext.getMetadataCollector().getGlobalRegistrations();
		final String generator = generatedValue.generator();

		final var globalSequenceMatch = globalRegistrations.getSequenceGeneratorRegistrations().get( generator );
		if ( globalSequenceMatch != null ) {
			throw new MappingException(
					String.format(
							Locale.ROOT,
							"@GeneratedValue for %s (%s) specified TABLE generation, but referred to a @SequenceGenerator",
							entityMapping.getEntityName(),
							generator
					)
			);
		}

		final var globalGenericMatch = globalRegistrations.getGenericGeneratorRegistrations().get( generator );
		if ( globalGenericMatch != null ) {
			throw new MappingException(
					String.format(
							Locale.ROOT,
							"@GeneratedValue for %s (%s) specified TABLE generation, but referred to a @GenericGenerator",
							entityMapping.getEntityName(),
							generator
					)
			);
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// AUTO

	@Override
	protected void handleUnnamedAutoGenerator() {
		// todo (7.0) : null or entityMapping.getJpaEntityName() for "name from GeneratedValue"?

		final var classDetailsRegistry = buildingContext.getMetadataCollector().getClassDetailsRegistry();

		final var localizedSequenceMatch = findLocalizedMatch(
				JpaAnnotations.SEQUENCE_GENERATOR,
				idMember,
				classDetailsRegistry.getClassDetails( entityMapping.getClassName() ),
				null,
				null,
				buildingContext
		);
		if ( localizedSequenceMatch != null ) {
			handleSequenceGenerator( null, localizedSequenceMatch, idValue, idMember, buildingContext );
			return;
		}

		final var localizedTableMatch = findLocalizedMatch(
				JpaAnnotations.TABLE_GENERATOR,
				idMember,
				classDetailsRegistry.getClassDetails( entityMapping.getClassName() ),
				null,
				null,
				buildingContext
		);
		if ( localizedTableMatch != null ) {
			handleTableGenerator( null, localizedTableMatch );
			return;
		}

		final var localizedGenericMatch = findLocalizedMatch(
				HibernateAnnotations.GENERIC_GENERATOR,
				idMember,
				classDetailsRegistry.getClassDetails( entityMapping.getClassName() ),
				null,
				null,
				buildingContext
		);
		if ( localizedGenericMatch != null ) {
			GeneratorAnnotationHelper.handleGenericGenerator(
					entityMapping.getJpaEntityName(),
					localizedGenericMatch,
					entityMapping,
					idValue,
					buildingContext
			);
			return;
		}

		if ( handleAsMetaAnnotated() ) {
			return;
		}

		if ( idMember.getType().isImplementor( UUID.class )
				|| idMember.getType().isImplementor( String.class ) ) {
			GeneratorAnnotationHelper.handleUuidStrategy(
					idValue,
					idMember,
					classDetailsRegistry.getClassDetails( entityMapping.getClassName() ),
					buildingContext
			);
			return;
		}

		if ( handleAsLegacyGenerator() ) {
			return;
		}

		handleSequenceGenerator( null, null, idValue, idMember, buildingContext );
	}

	@Override
	protected void handleNamedAutoGenerator() {
		if ( handleAsLocalAutoGenerator() ) {
			return;
		}

		if ( handleAsNamedGlobalAutoGenerator() ) {
			return;
		}

		final String generator = generatedValue.generator();
		final var legacyNamedGenerator = mapLegacyNamedGenerator( generator, buildingContext );
		if ( legacyNamedGenerator != null ) {
			//generator settings
			GeneratorBinder.createGeneratorFrom(
					new IdentifierGeneratorDefinition( generator, legacyNamedGenerator.getName() ),
					idValue,
					buildingContext
			);
			return;
		}

		if ( handleAsMetaAnnotated() ) {
			return;
		}

		if ( idMember.getType().isImplementor( UUID.class )
				|| idMember.getType().isImplementor( String.class ) ) {
			GeneratorAnnotationHelper.handleUuidStrategy(
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

		handleSequenceGenerator( generator, null, idValue, idMember, buildingContext );
	}

	private boolean handleAsLocalAutoGenerator() {
		if ( "increment".equals( generatedValue.generator() ) ) {
			final var incrementGenerator =
					new GenericGeneratorAnnotation( buildingContext.getBootstrapContext().getModelsContext() );
			incrementGenerator.name( "increment" );
			incrementGenerator.strategy( "increment" );

			GeneratorAnnotationHelper.handleGenericGenerator(
					generatedValue.generator(),
					incrementGenerator,
					entityMapping,
					idValue,
					buildingContext
			);
			return true;
		}

		final String generator = generatedValue.generator();
		assert !generator.isEmpty();

		final var classDetailsRegistry = buildingContext.getMetadataCollector().getClassDetailsRegistry();

		final var localizedSequenceMatch = findLocalizedMatch(
				JpaAnnotations.SEQUENCE_GENERATOR,
				idMember,
				classDetailsRegistry.getClassDetails( entityMapping.getClassName() ),
				SequenceGenerator::name,
				generator,
				buildingContext
		);
		if ( localizedSequenceMatch != null ) {
			handleSequenceGenerator( generator, localizedSequenceMatch, idValue, idMember, buildingContext );
			return true;
		}

		final var localizedTableMatch = findLocalizedMatch(
				JpaAnnotations.TABLE_GENERATOR,
				idMember,
				classDetailsRegistry.getClassDetails( entityMapping.getClassName() ),
				TableGenerator::name,
				generator,
				buildingContext
		);
		if ( localizedTableMatch != null ) {
			handleTableGenerator(generator, localizedTableMatch );
			return true;
		}

		final var localizedGenericMatch = findLocalizedMatch(
				HibernateAnnotations.GENERIC_GENERATOR,
				idMember,
				classDetailsRegistry.getClassDetails( entityMapping.getClassName() ),
				GenericGenerator::name,
				generator,
				buildingContext
		);
		if ( localizedGenericMatch != null ) {
			GeneratorAnnotationHelper.handleGenericGenerator(
					generator,
					localizedGenericMatch,
					entityMapping,
					idValue,
					buildingContext
			);
			return true;
		}

		return false;
	}

	private boolean handleAsNamedGlobalAutoGenerator() {
		final var globalRegistrations = buildingContext.getMetadataCollector().getGlobalRegistrations();
		final String generator = generatedValue.generator();

		final var globalSequenceMatch = globalRegistrations.getSequenceGeneratorRegistrations().get( generator );
		if ( globalSequenceMatch != null ) {
			handleSequenceGenerator( generator, globalSequenceMatch.configuration(), idValue, idMember, buildingContext );
			return true;
		}

		final var globalTableMatch = globalRegistrations.getTableGeneratorRegistrations().get( generator );
		if ( globalTableMatch != null ) {
			handleTableGenerator( generator, globalTableMatch.configuration() );
			return true;
		}

		final var globalGenericMatch = globalRegistrations.getGenericGeneratorRegistrations().get( generator );
		if ( globalGenericMatch != null ) {
			GeneratorAnnotationHelper.handleGenericGenerator(
					generator,
					globalGenericMatch.configuration(),
					entityMapping,
					idValue,
					buildingContext
			);
			return true;
		}

		return false;
	}

	private void handleTableGenerator(String nameFromGeneratedValue, TableGenerator generatorAnnotation) {
		GeneratorAnnotationHelper.handleTableGenerator(
				nameFromGeneratedValue,
				generatorAnnotation,
				idValue,
				idMember,
				buildingContext
		);
	}

}
