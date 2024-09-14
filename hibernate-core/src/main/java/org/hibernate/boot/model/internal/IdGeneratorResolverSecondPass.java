/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.internal;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.hibernate.MappingException;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.IdGeneratorType;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.relational.ExportableProducer;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.spi.GenericGeneratorRegistration;
import org.hibernate.boot.models.spi.SequenceGeneratorRegistration;
import org.hibernate.boot.models.spi.TableGeneratorRegistration;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.generator.Generator;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.id.OptimizableGenerator;
import org.hibernate.id.enhanced.LegacyNamingStrategy;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.enhanced.SingleNamingStrategy;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.resource.beans.container.spi.BeanContainer;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;

import static org.hibernate.boot.model.internal.GeneratorBinder.callConfigure;
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
			case IDENTITY -> GeneratorAnnotationHelper.handleIdentityStrategy( idValue, idMember, buildingContext );
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
		final SequenceGenerator localizedMatch = GeneratorAnnotationHelper.findLocalizedMatch(
				JpaAnnotations.SEQUENCE_GENERATOR,
				idMember,
				SequenceGenerator::name,
				generatedValue.generator(),
				buildingContext
		);
		if ( localizedMatch != null ) {
			handleSequenceGenerator( generatedValue.generator(), localizedMatch );
			return;
		}

		// look for the matching global registration, if one.
		final SequenceGeneratorRegistration globalMatch = buildingContext.getMetadataCollector()
				.getGlobalRegistrations()
				.getSequenceGeneratorRegistrations()
				.get( generatedValue.generator() );
		if ( globalMatch != null ) {
			handleSequenceGenerator( generatedValue.generator(), globalMatch.configuration() );
			return;
		}

		validateSequenceGeneration();

		handleSequenceGenerator( generatedValue.generator(), null );
	}

	private void validateSequenceGeneration() {
		// basically, make sure there is neither a TableGenerator nor GenericGenerator with this name

		final TableGeneratorRegistration globalTableMatch = buildingContext.getMetadataCollector()
				.getGlobalRegistrations()
				.getTableGeneratorRegistrations()
				.get( generatedValue.generator() );
		if ( globalTableMatch != null ) {
			throw new MappingException(
					String.format(
							Locale.ROOT,
							"@GeneratedValue for %s (%s) specified SEQUENCE generation, but referred to a @TableGenerator",
							entityMapping.getEntityName(),
							generatedValue.generator()
					)
			);
		}

		final GenericGeneratorRegistration globalGenericMatch = buildingContext.getMetadataCollector()
				.getGlobalRegistrations()
				.getGenericGeneratorRegistrations()
				.get( generatedValue.generator() );
		if ( globalGenericMatch != null ) {
			throw new MappingException(
					String.format(
							Locale.ROOT,
							"@GeneratedValue for %s (%s) specified SEQUENCE generation, but referred to a @GenericGenerator",
							entityMapping.getEntityName(),
							generatedValue.generator()
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
		if ( localizedMatch != null ) {
			handleTableGenerator( null, localizedMatch );
			return;
		}

		handleTableGenerator( null, null );
	}

	private void handleNamedTableGenerator() {
		final TableGenerator localizedTableMatch = GeneratorAnnotationHelper.findLocalizedMatch(
				JpaAnnotations.TABLE_GENERATOR,
				idMember,
				TableGenerator::name,
				generatedValue.generator(),
				buildingContext
		);
		if ( localizedTableMatch != null ) {
			handleTableGenerator( generatedValue.generator(), localizedTableMatch );
			return;
		}

		// look for the matching global registration, if one.
		final TableGeneratorRegistration globalMatch = buildingContext.getMetadataCollector()
				.getGlobalRegistrations()
				.getTableGeneratorRegistrations()
				.get( generatedValue.generator() );
		if ( globalMatch != null ) {
			handleTableGenerator( generatedValue.generator(), globalMatch.configuration() );
			return;
		}

		validateTableGeneration();

		handleTableGenerator( generatedValue.generator(), null );
	}

	private void validateTableGeneration() {
		// basically, make sure there is neither a SequenceGenerator nor a GenericGenerator with this name

		final SequenceGeneratorRegistration globalSequenceMatch = buildingContext.getMetadataCollector()
				.getGlobalRegistrations()
				.getSequenceGeneratorRegistrations()
				.get( generatedValue.generator() );
		if ( globalSequenceMatch != null ) {
			throw new MappingException(
					String.format(
							Locale.ROOT,
							"@GeneratedValue for %s (%s) specified TABLE generation, but referred to a @SequenceGenerator",
							entityMapping.getEntityName(),
							generatedValue.generator()
					)
			);
		}

		final GenericGeneratorRegistration globalGenericMatch = buildingContext.getMetadataCollector()
				.getGlobalRegistrations()
				.getGenericGeneratorRegistrations()
				.get( generatedValue.generator() );
		if ( globalGenericMatch != null ) {
			throw new MappingException(
					String.format(
							Locale.ROOT,
							"@GeneratedValue for %s (%s) specified TABLE generation, but referred to a @GenericGenerator",
							entityMapping.getEntityName(),
							generatedValue.generator()
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
					idMember,
					buildingContext
			);
			return;
		}

		if ( idMember.getType().isImplementor( UUID.class )
				|| idMember.getType().isImplementor( String.class ) ) {
			GeneratorAnnotationHelper.handleUuidStrategy( idValue, idMember, buildingContext );
			return;
		}

		handleSequenceGenerator( null, null );
	}

	private void handleNamedAutoGenerator() {
		if ( handleAsLocalAutoGenerator() ) {
			return;
		}

		if ( handleAsNamedGlobalAutoGenerator() ) {
			return;
		}

		final Class<? extends Generator> legacyNamedGenerator = mapLegacyNamedGenerator( generatedValue.generator(), buildingContext );
		if ( legacyNamedGenerator != null ) {
			//generator settings
			GeneratorBinder.createGeneratorFrom(
					new IdentifierGeneratorDefinition( generatedValue.generator(), legacyNamedGenerator.getName() ),
					idMember,
					idValue,
					entityMapping,
					buildingContext
			);
			return;
		}

		final List<? extends Annotation> metaAnnotated = idMember.getMetaAnnotated( IdGeneratorType.class, buildingContext.getMetadataCollector().getSourceModelBuildingContext() );
		if ( CollectionHelper.size( metaAnnotated ) > 0 ) {
			final Annotation generatorAnnotation = metaAnnotated.get( 0 );
			final IdGeneratorType markerAnnotation = generatorAnnotation.annotationType().getAnnotation( IdGeneratorType.class );
			idValue.setCustomIdGeneratorCreator( (creationContext) -> {

				final BeanContainer beanContainer = GeneratorBinder.beanContainer( buildingContext );
				final Generator identifierGenerator = GeneratorBinder.instantiateGenerator(
						beanContainer,
						markerAnnotation.value()
				);
				final Map<String,Object> configuration = new HashMap<>();
				GeneratorParameters.collectParameters(
						idValue,
						buildingContext.getMetadataCollector().getDatabase().getDialect(),
						entityMapping.getRootClass(),
						configuration::put
				);
				callConfigure( creationContext, identifierGenerator, configuration, idValue );
				return identifierGenerator;
			} );
			return;
		}

		if ( idMember.getType().isImplementor( UUID.class )
				|| idMember.getType().isImplementor( String.class ) ) {
			GeneratorAnnotationHelper.handleUuidStrategy( idValue, idMember, buildingContext );
			return;
		}

		handleSequenceGenerator( generatedValue.generator(), null );
	}

	private boolean handleAsLocalAutoGenerator() {
		assert !generatedValue.generator().isEmpty();

		final SequenceGenerator localizedSequenceMatch = GeneratorAnnotationHelper.findLocalizedMatch(
				JpaAnnotations.SEQUENCE_GENERATOR,
				idMember,
				SequenceGenerator::name,
				generatedValue.generator(),
				buildingContext
		);
		if ( localizedSequenceMatch != null ) {
			handleSequenceGenerator( generatedValue.generator(), localizedSequenceMatch );
			return true;
		}

		final TableGenerator localizedTableMatch = GeneratorAnnotationHelper.findLocalizedMatch(
				JpaAnnotations.TABLE_GENERATOR,
				idMember,
				TableGenerator::name,
				generatedValue.generator(),
				buildingContext
		);
		if ( localizedTableMatch != null ) {
			handleTableGenerator( generatedValue.generator(), localizedTableMatch );
			return true;
		}

		final GenericGenerator localizedGenericMatch = GeneratorAnnotationHelper.findLocalizedMatch(
				HibernateAnnotations.GENERIC_GENERATOR,
				idMember,
				GenericGenerator::name,
				generatedValue.generator(),
				buildingContext
		);
		if ( localizedGenericMatch != null ) {
			GeneratorAnnotationHelper.handleGenericGenerator(
					generatedValue.generator(),
					localizedGenericMatch,
					entityMapping,
					idValue,
					idMember,
					buildingContext
			);
			return true;
		}

		return false;
	}

	private boolean handleAsNamedGlobalAutoGenerator() {
		final SequenceGeneratorRegistration globalSequenceMatch = buildingContext.getMetadataCollector()
				.getGlobalRegistrations()
				.getSequenceGeneratorRegistrations()
				.get( generatedValue.generator() );
		if ( globalSequenceMatch != null ) {
			handleSequenceGenerator( generatedValue.generator(), globalSequenceMatch.configuration() );
			return true;
		}

		final TableGeneratorRegistration globalTableMatch = buildingContext.getMetadataCollector()
				.getGlobalRegistrations()
				.getTableGeneratorRegistrations()
				.get( generatedValue.generator() );
		if ( globalTableMatch != null ) {
			handleTableGenerator( generatedValue.generator(), globalTableMatch.configuration() );
			return true;
		}

		final GenericGeneratorRegistration globalGenericMatch = buildingContext.getMetadataCollector()
				.getGlobalRegistrations()
				.getGenericGeneratorRegistrations()
				.get( generatedValue.generator() );
		if ( globalGenericMatch != null ) {
			GeneratorAnnotationHelper.handleGenericGenerator(
					generatedValue.generator(),
					globalGenericMatch.configuration(),
					entityMapping,
					idValue,
					idMember,
					buildingContext
			);
			return true;
		}

		return false;
	}

	private void handleSequenceGenerator(String nameFromGeneratedValue, SequenceGenerator generator) {
		final Map<String, Object> configuration = extractConfiguration( nameFromGeneratedValue, generator );
		createGeneratorFrom( SequenceStyleGenerator.class, configuration );
	}

	private Map<String,Object> extractConfiguration(String nameFromGenerated, SequenceGenerator generator) {
		final Map<String, Object> configuration = new HashMap<>();
		if ( generator != null ) {
			configuration.put( GENERATOR_NAME, generator.name() );
		}
		else if ( nameFromGenerated != null ) {
			configuration.put( GENERATOR_NAME, nameFromGenerated );
		}

		applyCommonConfiguration( configuration, generator );

		if ( generator != null ) {
			SequenceStyleGenerator.applyConfiguration( generator, idValue, configuration::put );
		}

		return configuration;
	}

	private void applyCommonConfiguration(Map<String, Object> configuration, Annotation generatorAnnotation) {
		GeneratorParameters.collectParameters(
				idValue,
				buildingContext.getMetadataCollector().getDatabase().getDialect(),
				entityMapping.getRootClass(),
				configuration::put
		);

		// we need to better handle default allocation-size here...
		configuration.put( INCREMENT_PARAM, fallbackAllocationSize( buildingContext, generatorAnnotation ) );
	}

	private static int fallbackAllocationSize(MetadataBuildingContext buildingContext, Annotation generatorAnnotation) {
		if ( generatorAnnotation == null ) {
			// Special case where we have no matching SequenceGenerator/TableGenerator annotation.
			// Historically we interpreted such cases using a default of 1, but JPA says the default
			// here should be 50.  As a migration aid, under the assumption that one of the legacy
			// naming-strategies are used in such cases, we revert to the old default; otherwise we
			// use the compliant value.
			final StandardServiceRegistry serviceRegistry = buildingContext.getBootstrapContext().getServiceRegistry();
			final ConfigurationService configService = serviceRegistry.requireService( ConfigurationService.class );
			final String idNamingStrategy = configService.getSetting(
					AvailableSettings.ID_DB_STRUCTURE_NAMING_STRATEGY,
					StandardConverters.STRING,
					null
			);
			if ( LegacyNamingStrategy.STRATEGY_NAME.equals( idNamingStrategy )
					|| LegacyNamingStrategy.class.getName().equals( idNamingStrategy )
					|| SingleNamingStrategy.STRATEGY_NAME.equals( idNamingStrategy )
					|| SingleNamingStrategy.class.getName().equals( idNamingStrategy ) ) {
				return 1;
			}
		}

		return OptimizableGenerator.DEFAULT_INCREMENT_SIZE;
	}

	private void handleTableGenerator(String nameFromGeneratedValue, TableGenerator generator) {
		final Map<String, Object> configuration = extractConfiguration( nameFromGeneratedValue, generator );
		createGeneratorFrom( org.hibernate.id.enhanced.TableGenerator.class, configuration );
	}

	private Map<String,Object> extractConfiguration(String nameFromGenerated, TableGenerator generator) {
		final Map<String, Object> configuration = new HashMap<>();
		if ( generator != null ) {
			configuration.put( GENERATOR_NAME, generator.name() );
		}
		else if ( nameFromGenerated != null ) {
			configuration.put( GENERATOR_NAME, nameFromGenerated );
		}

		applyCommonConfiguration( configuration, generator );

		if ( generator != null ) {
			org.hibernate.id.enhanced.TableGenerator.applyConfiguration( generator, idValue, configuration::put );
		}

		return configuration;
	}

	private void createGeneratorFrom(
			Class<? extends Generator> generatorClass,
			Map<String, Object> configuration) {
		final BeanContainer beanContainer = GeneratorBinder.beanContainer( buildingContext );
		idValue.setCustomIdGeneratorCreator( (creationContext) -> {
			final Generator identifierGenerator = GeneratorBinder.instantiateGenerator( beanContainer, generatorClass );
			callConfigure( creationContext, identifierGenerator, configuration, idValue );
			if ( identifierGenerator instanceof IdentityGenerator ) {
				idValue.setColumnToIdentity();
			}

			// if we get here we have either a sequence or table generator,
			// both of which are ExportableProducers
			( (ExportableProducer) identifierGenerator ).registerExportables( creationContext.getDatabase() );

			return identifierGenerator;
		} );
	}
}
