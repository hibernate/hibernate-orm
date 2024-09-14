/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.generator.Generator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.mapping.IdentifierBag;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.models.spi.MemberDetails;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;

import static org.hibernate.boot.model.internal.GeneratorAnnotationHelper.applyBaselineConfiguration;
import static org.hibernate.boot.model.internal.GeneratorBinder.createGeneratorFrom;
import static org.hibernate.boot.model.internal.GeneratorBinder.makeIdGenerator;

/**
 * IdGeneratorResolver for handling generators assigned to id-bag mappings
 *
 * @author Andrea Boriero
 */
public class IdBagIdGeneratorResolverSecondPass implements IdGeneratorResolver {
	private final PersistentClass entityMapping;
	private final SimpleValue idValue;
	private final MemberDetails idAttributeMember;
	private final String generatorType;
	private final String generatorName;
	private final MetadataBuildingContext buildingContext;
	private final Map<String,String> configuration;

	public IdBagIdGeneratorResolverSecondPass(
			IdentifierBag idBagMapping,
			SimpleValue idValue,
			MemberDetails idAttributeMember,
			String generatorType,
			String generatorName,
			MetadataBuildingContext buildingContext) {
		this.entityMapping = null;
		this.idValue = idValue;
		this.idAttributeMember = idAttributeMember;
		this.generatorType = generatorType;
		this.generatorName = generatorName;
		this.buildingContext = buildingContext;

		this.configuration = new HashMap<>();
	}

	@Override
	public void doSecondPass(Map<String, PersistentClass> idGeneratorDefinitionMap) throws MappingException {
		final GeneratedValue generatedValue = idAttributeMember.getDirectAnnotationUsage( GeneratedValue.class );
		switch ( generatedValue.strategy() ) {
			case UUID -> GeneratorAnnotationHelper.handleUuidStrategy( idValue, idAttributeMember, buildingContext );
			case IDENTITY -> GeneratorAnnotationHelper.handleIdentityStrategy( idValue, idAttributeMember, buildingContext );
			case SEQUENCE -> handleSequenceStrategy(
					generatorName,
					idValue,
					idAttributeMember,
					buildingContext
			);
			case TABLE -> handleTableStrategy(
					generatorName,
					entityMapping,
					idValue,
					idAttributeMember,
					generatedValue,
					buildingContext
			);
			case AUTO -> handleAutoStrategy(
					generatorName,
					entityMapping,
					idValue,
					idAttributeMember,
					generatedValue,
					buildingContext
			);
		}
	}

	private void handleTableStrategy(
			String generatorName,
			PersistentClass entityMapping,
			SimpleValue idValue,
			MemberDetails idAttributeMember,
			GeneratedValue generatedValue,
			MetadataBuildingContext buildingContext) {
		final GlobalRegistrations globalRegistrations = buildingContext.getMetadataCollector().getGlobalRegistrations();
		final TableGeneratorRegistration globalTableGenerator = globalRegistrations.getTableGeneratorRegistrations().get( generatorName );
		if ( globalTableGenerator != null ) {
			handleTableGenerator(
					generatorName,
					globalTableGenerator.configuration(),
					configuration,
					idValue,
					idAttributeMember,
					buildingContext
			);
			return;
		}

		final TableGenerator localizedTableMatch = GeneratorAnnotationHelper.findLocalizedMatch(
				JpaAnnotations.TABLE_GENERATOR,
				idAttributeMember,
				TableGenerator::name,
				generatorName,
				buildingContext
		);
		if ( localizedTableMatch != null ) {
			handleTableGenerator( generatorName, localizedTableMatch, configuration, idValue, idAttributeMember, buildingContext );
			return;
		}

		GeneratorAnnotationHelper.handleTableGenerator(
				generatorName,
				new TableGeneratorJpaAnnotation( buildingContext.getMetadataCollector().getSourceModelBuildingContext() ),
				entityMapping,
				idValue,
				idAttributeMember,
				buildingContext
		);
	}

	private void handleSequenceStrategy(
			String generatorName,
			SimpleValue idValue,
			MemberDetails idAttributeMember,
			MetadataBuildingContext buildingContext) {
		final GlobalRegistrations globalRegistrations = buildingContext.getMetadataCollector().getGlobalRegistrations();

		final SequenceGeneratorRegistration globalSequenceGenerator = globalRegistrations.getSequenceGeneratorRegistrations().get( generatorName );
		if ( globalSequenceGenerator != null ) {
			handleSequenceGenerator(
					generatorName,
					globalSequenceGenerator.configuration(),
					configuration,
					idValue,
					idAttributeMember,
					buildingContext
			);
			return;
		}

		final SequenceGenerator localizedSequencedMatch = GeneratorAnnotationHelper.findLocalizedMatch(
				JpaAnnotations.SEQUENCE_GENERATOR,
				idAttributeMember,
				SequenceGenerator::name,
				generatorName,
				buildingContext
		);
		if ( localizedSequencedMatch != null ) {
			handleSequenceGenerator( generatorName, localizedSequencedMatch, configuration, idValue, idAttributeMember, buildingContext );
			return;
		}

		handleSequenceGenerator(
				generatorName,
				new SequenceGeneratorJpaAnnotation( buildingContext.getMetadataCollector().getSourceModelBuildingContext() ),
				configuration,
				idValue,
				idAttributeMember,
				buildingContext
		);
	}

	private void handleAutoStrategy(
			String generatorName,
			PersistentClass entityMapping,
			SimpleValue idValue,
			MemberDetails idAttributeMember,
			GeneratedValue generatedValue,
			MetadataBuildingContext buildingContext) {
		final GlobalRegistrations globalRegistrations = buildingContext.getMetadataCollector().getGlobalRegistrations();

		final SequenceGeneratorRegistration globalSequenceGenerator = globalRegistrations.getSequenceGeneratorRegistrations().get( generatorName );
		if ( globalSequenceGenerator != null ) {
			handleSequenceGenerator(
					generatorName,
					globalSequenceGenerator.configuration(),
					configuration,
					idValue,
					idAttributeMember,
					buildingContext
			);
			return;
		}

		final TableGeneratorRegistration globalTableGenerator = globalRegistrations.getTableGeneratorRegistrations().get( generatorName );
		if ( globalTableGenerator != null ) {
			handleTableGenerator(
					generatorName,
					globalTableGenerator.configuration(),
					configuration,
					idValue,
					idAttributeMember,
					buildingContext
			);
			return;
		}


		final Class<? extends Generator> legacyNamedGenerator = GeneratorStrategies.mapLegacyNamedGenerator( generatorName, idValue );
		if ( legacyNamedGenerator != null ) {
			//generator settings
			if ( idValue.getColumnSpan() == 1 ) {
				configuration.put( PersistentIdentifierGenerator.PK, idValue.getColumns().get(0).getName() );
			}
			createGeneratorFrom(
					new IdentifierGeneratorDefinition( generatorName, legacyNamedGenerator.getName(), configuration ),
					idAttributeMember,
					idValue,
					(Map) configuration,
					buildingContext
			);
			return;
		}


		final SequenceGenerator localizedSequencedMatch = GeneratorAnnotationHelper.findLocalizedMatch(
				JpaAnnotations.SEQUENCE_GENERATOR,
				idAttributeMember,
				SequenceGenerator::name,
				generatorName,
				buildingContext
		);
		if ( localizedSequencedMatch != null ) {
			handleSequenceGenerator( generatorName, localizedSequencedMatch, configuration, idValue, idAttributeMember, buildingContext );
			return;
		}

		final TableGenerator localizedTableMatch = GeneratorAnnotationHelper.findLocalizedMatch(
				JpaAnnotations.TABLE_GENERATOR,
				idAttributeMember,
				TableGenerator::name,
				generatorName,
				buildingContext
		);
		if ( localizedTableMatch != null ) {
			handleTableGenerator( generatorName, localizedTableMatch, configuration, idValue, idAttributeMember, buildingContext );
			return;
		}

		makeIdGenerator( idValue, idAttributeMember, generatorType, generatorName, buildingContext, null );
	}

	public static void handleSequenceGenerator(
			String generatorName,
			SequenceGenerator generatorConfig,
			Map<String,String> configuration,
			SimpleValue idValue,
			MemberDetails idMember,
			MetadataBuildingContext context) {
		applyBaselineConfiguration( generatorConfig, idValue, null, context, configuration::put );
		SequenceStyleGenerator.applyConfiguration( generatorConfig, idValue, configuration::put );

		createGeneratorFrom(
				new IdentifierGeneratorDefinition( generatorName, SequenceStyleGenerator.class.getName(), configuration ),
				idMember,
				idValue,
				(Map) configuration,
				context
		);

	}

	public static void handleTableGenerator(
			String generatorName,
			TableGenerator generatorConfig,
			Map<String,String> configuration,
			SimpleValue idValue,
			MemberDetails idMember,
			MetadataBuildingContext context) {
		GeneratorAnnotationHelper.applyBaselineConfiguration( generatorConfig, idValue, null, context, configuration::put );
		org.hibernate.id.enhanced.TableGenerator.applyConfiguration( generatorConfig, idValue, configuration::put );

		createGeneratorFrom(
				new IdentifierGeneratorDefinition( generatorName, org.hibernate.id.enhanced.TableGenerator.class.getName(), configuration ),
				idMember,
				idValue,
				(Map) configuration,
				context
		);

	}
}
