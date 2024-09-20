/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.hibernate.MappingException;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.IdGeneratorType;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.spi.GenericGeneratorRegistration;
import org.hibernate.boot.models.spi.GlobalRegistrations;
import org.hibernate.boot.models.spi.SequenceGeneratorRegistration;
import org.hibernate.boot.models.spi.TableGeneratorRegistration;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.generator.Generator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.resource.beans.container.spi.BeanContainer;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;

import static org.hibernate.boot.model.internal.GeneratorParameters.fallbackAllocationSize;
import static org.hibernate.boot.model.internal.GeneratorStrategies.mapLegacyNamedGenerator;
import static org.hibernate.id.IdentifierGenerator.GENERATOR_NAME;
import static org.hibernate.id.OptimizableGenerator.INCREMENT_PARAM;

/**
 * SecondPass implementing delayed resolution of id-generators associated with an entity.
 *
 * @see StrictIdGeneratorResolverSecondPass
 *
 * @author Steve Ebersole
 */
public class IdGeneratorResolverSecondPass implements IdGeneratorResolver {
	private final PersistentClass entityMapping;
	private final SimpleValue idValue;
	private final MemberDetails idMember;
	private final GeneratedValue generatedValue;
	private final MetadataBuildingContext buildingContext;

	public IdGeneratorResolverSecondPass(
			PersistentClass entityMapping,
			SimpleValue idValue,
			MemberDetails idMember,
			GeneratedValue generatedValue,
			MetadataBuildingContext buildingContext) {
		this.entityMapping = entityMapping;
		this.idValue = idValue;
		this.idMember = idMember;
		this.generatedValue = generatedValue;
		this.buildingContext = buildingContext;
	}

	@Override
	public void doSecondPass(Map<String, PersistentClass> persistentClasses) throws MappingException {
		switch ( generatedValue.strategy() ) {
			case UUID -> GeneratorAnnotationHelper.handleUuidStrategy( idValue, idMember, buildingContext );
			case IDENTITY -> GeneratorAnnotationHelper.handleIdentityStrategy( idValue );
			case SEQUENCE -> handleSequenceStrategy();
			case TABLE -> handleTableStrategy();
			case AUTO -> handleAutoStrategy();
		}
	}

	private void handleSequenceStrategy() {
		if ( generatedValue.generator().isEmpty() ) {
			handleUnnamedSequenceGenerator();
		}
		else {
			handleNamedSequenceGenerator();
		}
	}

	private void handleUnnamedSequenceGenerator() {
		// todo (7.0) : null or entityMapping.getJpaEntityName() for "name from GeneratedValue"?

		final SequenceGenerator localizedMatch = GeneratorAnnotationHelper.findLocalizedMatch(
				JpaAnnotations.SEQUENCE_GENERATOR,
				idMember,
				null,
				null,
				buildingContext
		);
		if ( localizedMatch != null ) {
			handleSequenceGenerator( null, localizedMatch );
			return;
		}

		handleSequenceGenerator( null, null );
	}

	private void handleNamedSequenceGenerator() {
		final String generator = generatedValue.generator();

		final SequenceGenerator localizedMatch = GeneratorAnnotationHelper.findLocalizedMatch(
				JpaAnnotations.SEQUENCE_GENERATOR,
				idMember,
				SequenceGenerator::name,
				generator,
				buildingContext
		);
		if ( localizedMatch != null ) {
			handleSequenceGenerator( generator, localizedMatch );
			return;
		}

		// look for the matching global registration, if one.
		final SequenceGeneratorRegistration globalMatch =
				buildingContext.getMetadataCollector()
						.getGlobalRegistrations()
						.getSequenceGeneratorRegistrations()
						.get( generator );
		if ( globalMatch != null ) {
			handleSequenceGenerator( generator, globalMatch.configuration() );
			return;
		}

		validateSequenceGeneration();

		handleSequenceGenerator( generator, null );
	}

	private void validateSequenceGeneration() {
		// basically, make sure there is neither a TableGenerator nor GenericGenerator with this name

		final GlobalRegistrations globalRegistrations =
				buildingContext.getMetadataCollector().getGlobalRegistrations();
		final String generator = generatedValue.generator();

		final TableGeneratorRegistration globalTableMatch =
				globalRegistrations.getTableGeneratorRegistrations().get( generator );
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

		final GenericGeneratorRegistration globalGenericMatch =
				globalRegistrations.getGenericGeneratorRegistrations().get( generator );
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

	private void handleTableStrategy() {
		if ( generatedValue.generator().isEmpty() ) {
			handleUnnamedTableGenerator();
		}
		else {
			handleNamedTableGenerator();
		}
	}

	private void handleUnnamedTableGenerator() {
		// todo (7.0) : null or entityMapping.getJpaEntityName() for "name from GeneratedValue"?

		final TableGenerator localizedMatch = GeneratorAnnotationHelper.findLocalizedMatch(
				JpaAnnotations.TABLE_GENERATOR,
				idMember,
				null,
				null,
				buildingContext
		);
		handleTableGenerator( null, localizedMatch );
	}

	private void handleNamedTableGenerator() {
		final String generator = generatedValue.generator();

		final TableGenerator localizedTableMatch = GeneratorAnnotationHelper.findLocalizedMatch(
				JpaAnnotations.TABLE_GENERATOR,
				idMember,
				TableGenerator::name,
				generator,
				buildingContext
		);
		if ( localizedTableMatch != null ) {
			handleTableGenerator( generator, localizedTableMatch );
			return;
		}

		// look for the matching global registration, if one.
		final TableGeneratorRegistration globalMatch =
				buildingContext.getMetadataCollector().getGlobalRegistrations()
						.getTableGeneratorRegistrations().get( generator );
		if ( globalMatch != null ) {
			handleTableGenerator( generator, globalMatch.configuration() );
			return;
		}

		validateTableGeneration();

		handleTableGenerator( generator, null );
	}

	private void validateTableGeneration() {
		// basically, make sure there is neither a SequenceGenerator nor a GenericGenerator with this name

		final GlobalRegistrations globalRegistrations =
				buildingContext.getMetadataCollector().getGlobalRegistrations();
		final String generator = generatedValue.generator();

		final SequenceGeneratorRegistration globalSequenceMatch =
				globalRegistrations.getSequenceGeneratorRegistrations().get( generator );
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

		final GenericGeneratorRegistration globalGenericMatch =
				globalRegistrations.getGenericGeneratorRegistrations().get( generator );
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

	private void handleAutoStrategy() {
		if ( generatedValue.generator().isEmpty() ) {
			handleUnnamedAutoGenerator();
		}
		else {
			handleNamedAutoGenerator();
		}
	}

	private void handleUnnamedAutoGenerator() {
		// todo (7.0) : null or entityMapping.getJpaEntityName() for "name from GeneratedValue"?

		final SequenceGenerator localizedSequenceMatch = GeneratorAnnotationHelper.findLocalizedMatch(
				JpaAnnotations.SEQUENCE_GENERATOR,
				idMember,
				null,
				null,
				buildingContext
		);
		if ( localizedSequenceMatch != null ) {
			handleSequenceGenerator( null, localizedSequenceMatch );
			return;
		}

		final TableGenerator localizedTableMatch = GeneratorAnnotationHelper.findLocalizedMatch(
				JpaAnnotations.TABLE_GENERATOR,
				idMember,
				null,
				null,
				buildingContext
		);
		if ( localizedTableMatch != null ) {
			handleTableGenerator( null, localizedTableMatch );
			return;
		}

		final GenericGenerator localizedGenericMatch = GeneratorAnnotationHelper.findLocalizedMatch(
				HibernateAnnotations.GENERIC_GENERATOR,
				idMember,
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
			GeneratorAnnotationHelper.handleUuidStrategy( idValue, idMember, buildingContext );
			return;
		}

		handleSequenceGenerator( null, null );
	}

	private boolean handleAsMetaAnnotated() {
		final Annotation fromMember = findGeneratorAnnotation( idMember );
		if ( fromMember != null ) {
			handleIdGeneratorType( fromMember );
			return true;
		}

		final Annotation fromClass = findGeneratorAnnotation( idMember.getDeclaringType() );
		if ( fromClass != null ) {
			handleIdGeneratorType( fromClass );
			return true;
		}

		final ClassDetails packageInfoDetails = GeneratorAnnotationHelper.locatePackageInfoDetails( idMember.getDeclaringType(), buildingContext );
		if ( packageInfoDetails != null ) {
			final Annotation fromPackage = findGeneratorAnnotation( packageInfoDetails );
			if ( fromPackage != null ) {
				handleIdGeneratorType( fromPackage );
				return true;
			}
		}

		return false;
	}

	private Annotation findGeneratorAnnotation(AnnotationTarget annotationTarget) {
		final List<? extends Annotation> metaAnnotated = annotationTarget.getMetaAnnotated( IdGeneratorType.class, buildingContext.getMetadataCollector().getSourceModelBuildingContext() );
		if ( CollectionHelper.size( metaAnnotated ) > 0 ) {
			return metaAnnotated.get( 0 );
		}

		return null;
	}

	private void handleIdGeneratorType(Annotation generatorAnnotation) {
		final IdGeneratorType markerAnnotation = generatorAnnotation.annotationType().getAnnotation( IdGeneratorType.class );
		idValue.setCustomIdGeneratorCreator( (creationContext) -> {
			final BeanContainer beanContainer = GeneratorBinder.beanContainer( buildingContext );
			final Generator identifierGenerator = GeneratorBinder.instantiateGenerator(
					beanContainer,
					markerAnnotation.value()
			);
			GeneratorAnnotationHelper.prepareForUse(
					identifierGenerator,
					generatorAnnotation,
					idMember,
					null,
					null,
					creationContext
			);
			return identifierGenerator;
		} );
	}

	private void handleNamedAutoGenerator() {
		if ( handleAsLocalAutoGenerator() ) {
			return;
		}

		if ( handleAsNamedGlobalAutoGenerator() ) {
			return;
		}

		final String generator = generatedValue.generator();
		final Class<? extends Generator> legacyNamedGenerator = mapLegacyNamedGenerator( generator, buildingContext );
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
			GeneratorAnnotationHelper.handleUuidStrategy( idValue, idMember, buildingContext );
			return;
		}

		handleSequenceGenerator(generator, null );
	}

	private boolean handleAsLocalAutoGenerator() {
		final String generator = generatedValue.generator();
		assert !generator.isEmpty();

		final SequenceGenerator localizedSequenceMatch = GeneratorAnnotationHelper.findLocalizedMatch(
				JpaAnnotations.SEQUENCE_GENERATOR,
				idMember,
				SequenceGenerator::name,
				generator,
				buildingContext
		);
		if ( localizedSequenceMatch != null ) {
			handleSequenceGenerator( generator, localizedSequenceMatch );
			return true;
		}

		final TableGenerator localizedTableMatch = GeneratorAnnotationHelper.findLocalizedMatch(
				JpaAnnotations.TABLE_GENERATOR,
				idMember,
				TableGenerator::name,
				generator,
				buildingContext
		);
		if ( localizedTableMatch != null ) {
			handleTableGenerator(generator, localizedTableMatch );
			return true;
		}

		final GenericGenerator localizedGenericMatch = GeneratorAnnotationHelper.findLocalizedMatch(
				HibernateAnnotations.GENERIC_GENERATOR,
				idMember,
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
		final GlobalRegistrations globalRegistrations =
				buildingContext.getMetadataCollector().getGlobalRegistrations();
		final String generator = generatedValue.generator();

		final SequenceGeneratorRegistration globalSequenceMatch =
				globalRegistrations.getSequenceGeneratorRegistrations().get( generator );
		if ( globalSequenceMatch != null ) {
			handleSequenceGenerator( generator, globalSequenceMatch.configuration() );
			return true;
		}

		final TableGeneratorRegistration globalTableMatch =
				globalRegistrations.getTableGeneratorRegistrations().get( generator );
		if ( globalTableMatch != null ) {
			handleTableGenerator( generator, globalTableMatch.configuration() );
			return true;
		}

		final GenericGeneratorRegistration globalGenericMatch =
				globalRegistrations.getGenericGeneratorRegistrations().get( generator );
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

	private void handleSequenceGenerator(String nameFromGeneratedValue, SequenceGenerator generatorAnnotation) {
		idValue.setCustomIdGeneratorCreator( (creationContext) -> {
			final BeanContainer beanContainer = GeneratorBinder.beanContainer( buildingContext );
			final SequenceStyleGenerator identifierGenerator = GeneratorBinder.instantiateGenerator(
					beanContainer,
					SequenceStyleGenerator.class
			);
			GeneratorAnnotationHelper.prepareForUse(
					identifierGenerator,
					generatorAnnotation,
					idMember,
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

	private void handleTableGenerator(String nameFromGeneratedValue, TableGenerator generatorAnnotation) {
		GeneratorAnnotationHelper.handleTableGenerator(
				nameFromGeneratedValue,
				generatorAnnotation,
				entityMapping,
				idValue,
				idMember,
				buildingContext
		);
	}

}
