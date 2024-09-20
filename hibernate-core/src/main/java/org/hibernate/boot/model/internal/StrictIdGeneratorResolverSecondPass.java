/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.hibernate.MappingException;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.models.annotations.internal.SequenceGeneratorJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.TableGeneratorJpaAnnotation;
import org.hibernate.boot.models.spi.GenericGeneratorRegistration;
import org.hibernate.boot.models.spi.GlobalRegistrations;
import org.hibernate.boot.models.spi.SequenceGeneratorRegistration;
import org.hibernate.boot.models.spi.TableGeneratorRegistration;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.generator.Generator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.resource.beans.container.spi.BeanContainer;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.SequenceGenerator;

import static org.hibernate.boot.model.internal.GeneratorAnnotationHelper.*;
import static org.hibernate.boot.model.internal.GeneratorAnnotationHelper.handleUuidStrategy;
import static org.hibernate.boot.model.internal.GeneratorParameters.fallbackAllocationSize;
import static org.hibernate.boot.model.internal.GeneratorParameters.identityTablesString;
import static org.hibernate.boot.model.internal.GeneratorStrategies.mapLegacyNamedGenerator;
import static org.hibernate.id.IdentifierGenerator.ENTITY_NAME;
import static org.hibernate.id.IdentifierGenerator.GENERATOR_NAME;
import static org.hibernate.id.IdentifierGenerator.JPA_ENTITY_NAME;
import static org.hibernate.id.OptimizableGenerator.IMPLICIT_NAME_BASE;
import static org.hibernate.id.OptimizableGenerator.INCREMENT_PARAM;
import static org.hibernate.id.PersistentIdentifierGenerator.PK;
import static org.hibernate.id.PersistentIdentifierGenerator.TABLE;
import static org.hibernate.id.PersistentIdentifierGenerator.TABLES;

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
public class StrictIdGeneratorResolverSecondPass implements IdGeneratorResolver {
	private final PersistentClass entityMapping;
	private final SimpleValue idValue;
	private final MemberDetails idMember;

	private final MetadataBuildingContext buildingContext;

	public StrictIdGeneratorResolverSecondPass(
			PersistentClass entityMapping,
			SimpleValue idValue,
			MemberDetails idMember,
			MetadataBuildingContext buildingContext) {
		this.entityMapping = entityMapping;
		this.idValue = idValue;
		this.idMember = idMember;
		this.buildingContext = buildingContext;
	}

	@Override
	public void doSecondPass(Map<String, PersistentClass> persistentClasses) throws MappingException {
		final GeneratedValue generatedValue = idMember.getDirectAnnotationUsage( GeneratedValue.class );
		switch ( generatedValue.strategy() ) {
			case UUID -> handleUuidStrategy( idValue, idMember, buildingContext );
			case IDENTITY -> handleIdentityStrategy( idValue );
			case SEQUENCE -> handleSequenceStrategy( generatedValue );
			case TABLE -> handleTableStrategy( generatedValue );
			case AUTO -> handleAutoStrategy( generatedValue );
		}
	}

	private void handleSequenceStrategy(GeneratedValue generatedValue) {
		if ( generatedValue.generator().isEmpty() ) {
			handleUnnamedSequenceGenerator();
		}
		else {
			handleNamedSequenceGenerator( generatedValue );
		}
	}

	private void handleUnnamedSequenceGenerator() {
		final InFlightMetadataCollector metadataCollector = buildingContext.getMetadataCollector();

		// according to the spec, this should locate a generator with the same name as the entity-name
		final SequenceGeneratorRegistration globalMatch =
				metadataCollector.getGlobalRegistrations().getSequenceGeneratorRegistrations()
						.get( entityMapping.getJpaEntityName() );
		if ( globalMatch != null ) {
			handleSequenceGenerator(
					entityMapping.getJpaEntityName(),
					globalMatch.configuration(),
					idValue,
					idMember,
					buildingContext
			);
			return;
		}

		handleSequenceGenerator(
				entityMapping.getJpaEntityName(),
				new SequenceGeneratorJpaAnnotation( metadataCollector.getSourceModelBuildingContext() ),
				idValue,
				idMember,
				buildingContext
		);
	}

	private void handleNamedSequenceGenerator(GeneratedValue generatedValue) {
		final InFlightMetadataCollector metadataCollector = buildingContext.getMetadataCollector();

		final SequenceGeneratorRegistration globalMatch =
				metadataCollector.getGlobalRegistrations()
						.getSequenceGeneratorRegistrations().get( generatedValue.generator() );
		if ( globalMatch != null ) {
			handleSequenceGenerator(
					generatedValue.generator(),
					globalMatch.configuration(),
					idValue,
					idMember,
					buildingContext
			);
			return;
		}

		handleSequenceGenerator(
				generatedValue.generator(),
				new SequenceGeneratorJpaAnnotation( generatedValue.generator(), metadataCollector.getSourceModelBuildingContext() ),
				idValue,
				idMember,
				buildingContext
		);
	}

	private void handleTableStrategy(GeneratedValue generatedValue) {
		if ( generatedValue.generator().isEmpty() ) {
			handleUnnamedTableGenerator();
		}
		else {
			handleNamedTableGenerator( generatedValue );
		}
	}

	private void handleUnnamedTableGenerator() {
		final InFlightMetadataCollector metadataCollector = buildingContext.getMetadataCollector();

		final TableGeneratorRegistration globalMatch =
				metadataCollector.getGlobalRegistrations().getTableGeneratorRegistrations()
						.get( entityMapping.getJpaEntityName() );
		if ( globalMatch != null ) {
			handleTableGenerator(
					entityMapping.getJpaEntityName(),
					globalMatch.configuration(),
					entityMapping,
					idValue,
					idMember,
					buildingContext
			);
			return;
		}

		handleTableGenerator(
				entityMapping.getJpaEntityName(),
				new TableGeneratorJpaAnnotation( metadataCollector.getSourceModelBuildingContext() ),
				entityMapping,
				idValue,
				idMember,
				buildingContext
		);
	}

	private void handleNamedTableGenerator(GeneratedValue generatedValue) {
		final InFlightMetadataCollector metadataCollector = buildingContext.getMetadataCollector();

		final TableGeneratorRegistration globalMatch =
				metadataCollector.getGlobalRegistrations().getTableGeneratorRegistrations()
						.get( generatedValue.generator() );
		if ( globalMatch != null ) {
			handleTableGenerator(
					generatedValue.generator(),
					globalMatch.configuration(),
					entityMapping,
					idValue,
					idMember,
					buildingContext
			);

			return;
		}

		handleTableGenerator(
				generatedValue.generator(),
				new TableGeneratorJpaAnnotation( generatedValue.generator(), metadataCollector.getSourceModelBuildingContext() ),
				entityMapping,
				idValue,
				idMember,
				buildingContext
		);
	}

	private void handleAutoStrategy(GeneratedValue generatedValue) {
		final String generator = generatedValue.generator();
		final String globalRegistrationName = generator.isEmpty() ? entityMapping.getJpaEntityName() : generator;

		final InFlightMetadataCollector metadataCollector = buildingContext.getMetadataCollector();
		final GlobalRegistrations globalRegistrations = metadataCollector.getGlobalRegistrations();

		final SequenceGeneratorRegistration globalSequenceMatch =
				globalRegistrations.getSequenceGeneratorRegistrations().get( globalRegistrationName );
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

		final TableGeneratorRegistration globalTableMatch =
				globalRegistrations.getTableGeneratorRegistrations().get( globalRegistrationName );
		if ( globalTableMatch != null ) {
			handleTableGenerator(
					globalRegistrationName,
					globalTableMatch.configuration(),
					entityMapping,
					idValue,
					idMember,
					buildingContext
			);
			return;
		}

		final GenericGeneratorRegistration globalGenericMatch =
				globalRegistrations.getGenericGeneratorRegistrations().get( globalRegistrationName );
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

		// Implicit handling of UUID generation
		if ( idMember.getType().isImplementor( UUID.class )
				|| idMember.getType().isImplementor( String.class ) ) {
			handleUuidStrategy( idValue, idMember, buildingContext );
			return;
		}


		// Handle a few legacy Hibernate generators...
		if ( !generator.isEmpty() ) {
			final Class<? extends Generator> legacyNamedGenerator = mapLegacyNamedGenerator( generator, idValue );
			if ( legacyNamedGenerator != null ) {
				final Map<String,String> configuration = buildLegacyGeneratorConfig();
				//noinspection unchecked,rawtypes
				GeneratorBinder.createGeneratorFrom(
						new IdentifierGeneratorDefinition( generator, legacyNamedGenerator.getName(), configuration ),
						idValue,
						(Map) configuration,
						buildingContext
				);
				return;
			}
		}

		handleSequenceGenerator(
				globalRegistrationName,
				new SequenceGeneratorJpaAnnotation( generator, metadataCollector.getSourceModelBuildingContext() ),
				idValue,
				idMember,
				buildingContext
		);
	}

	private HashMap<String, String> buildLegacyGeneratorConfig() {
		final Database database = buildingContext.getMetadataCollector().getDatabase();
		final Dialect dialect = database.getDialect();

		final HashMap<String, String> configuration = new HashMap<>();

		final String tableName = idValue.getTable().getQuotedName( dialect );
		configuration.put( TABLE, tableName );

		final Column idColumn = (Column) idValue.getSelectables().get( 0);
		final String idColumnName = idColumn.getQuotedName( dialect );
		configuration.put( PK, idColumnName );

		configuration.put( ENTITY_NAME, entityMapping.getEntityName() );
		configuration.put( JPA_ENTITY_NAME, entityMapping.getJpaEntityName() );

		// The table name is not really a good default for subselect entities,
		// so use the JPA entity name which is short
		configuration.put(
				IMPLICIT_NAME_BASE,
				idValue.getTable().isSubselect()
						? entityMapping.getJpaEntityName()
						: idValue.getTable().getName()
		);

		configuration.put( TABLES, identityTablesString( dialect, entityMapping.getRootClass() ) );

		return configuration;
	}

	public static void handleSequenceGenerator(
			String nameFromGeneratedValue,
			SequenceGenerator generatorAnnotation,
			SimpleValue idValue,
			MemberDetails idMember,
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
}
