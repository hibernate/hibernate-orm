/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiConsumer;

import org.hibernate.Internal;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.id.Configurable;
import org.hibernate.id.OptimizableGenerator;
import org.hibernate.id.enhanced.LegacyNamingStrategy;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.enhanced.SingleNamingStrategy;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.RootClass;

import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;
import org.hibernate.mapping.Value;

import static org.hibernate.cfg.MappingSettings.ID_DB_STRUCTURE_NAMING_STRATEGY;
import static org.hibernate.cfg.MappingSettings.PREFERRED_POOLED_OPTIMIZER;
import static org.hibernate.id.IdentifierGenerator.CONTRIBUTOR_NAME;
import static org.hibernate.id.IdentifierGenerator.ENTITY_NAME;
import static org.hibernate.id.IdentifierGenerator.JPA_ENTITY_NAME;
import static org.hibernate.id.OptimizableGenerator.DEFAULT_INITIAL_VALUE;
import static org.hibernate.id.OptimizableGenerator.IMPLICIT_NAME_BASE;
import static org.hibernate.id.OptimizableGenerator.INCREMENT_PARAM;
import static org.hibernate.id.OptimizableGenerator.INITIAL_PARAM;
import static org.hibernate.id.PersistentIdentifierGenerator.CATALOG;
import static org.hibernate.id.PersistentIdentifierGenerator.OPTIONS;
import static org.hibernate.id.PersistentIdentifierGenerator.PK;
import static org.hibernate.id.PersistentIdentifierGenerator.SCHEMA;
import static org.hibernate.id.PersistentIdentifierGenerator.TABLE;
import static org.hibernate.id.PersistentIdentifierGenerator.TABLES;
import static org.hibernate.boot.BootLogging.BOOT_LOGGER;
import static org.hibernate.id.enhanced.SequenceStyleGenerator.SEQUENCE_PARAM;
import static org.hibernate.id.enhanced.TableGenerator.CONFIG_PREFER_SEGMENT_PER_ENTITY;
import static org.hibernate.id.enhanced.TableGenerator.SEGMENT_COLUMN_PARAM;
import static org.hibernate.id.enhanced.TableGenerator.SEGMENT_VALUE_PARAM;
import static org.hibernate.id.enhanced.TableGenerator.TABLE_PARAM;
import static org.hibernate.id.enhanced.TableGenerator.VALUE_COLUMN_PARAM;
import static org.hibernate.internal.util.StringHelper.isNotBlank;
import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;

/**
 * Responsible for setting up the parameters which are passed to
 * {@link Configurable#configure(GeneratorCreationContext, Properties)}
 * when a {@link Configurable} generator is instantiated.
 *
 * @since 7.0
 *
 * @author Gavin King
 */
public class GeneratorParameters {

	/**
	 * Collect the parameters which should be passed to
	 * {@link Configurable#configure(GeneratorCreationContext, Properties)}.
	 */
	public static Properties collectParameters(
			Value identifierValue,
			Dialect dialect,
			RootClass rootClass,
			Map<String, Object> configuration,
			ConfigurationService configService) {
		final var params = new Properties();
		collectParameters( identifierValue, dialect, rootClass, params::put, configService );
		if ( configuration != null ) {
			params.putAll( configuration );
		}
		return params;
	}

	public static void collectParameters(
			Value identifierValue,
			Dialect dialect,
			RootClass rootClass,
			BiConsumer<String, String> parameterCollector,
			ConfigurationService configService) {
		// default initial value and allocation size per-JPA defaults
		parameterCollector.accept( INITIAL_PARAM, String.valueOf( DEFAULT_INITIAL_VALUE ) );
		parameterCollector.accept( INCREMENT_PARAM,	String.valueOf( defaultIncrement( configService ) ) );

		collectBaselineProperties( identifierValue, dialect, rootClass, parameterCollector, configService );
	}

	public static int fallbackAllocationSize(Annotation generatorAnnotation, MetadataBuildingContext buildingContext) {
		if ( generatorAnnotation == null ) {
			final var configService = buildingContext.getBootstrapContext().getConfigurationService();
			final String idNamingStrategy = configService.getSetting( ID_DB_STRUCTURE_NAMING_STRATEGY, StandardConverters.STRING );
			if ( LegacyNamingStrategy.STRATEGY_NAME.equals( idNamingStrategy )
					|| LegacyNamingStrategy.class.getName().equals( idNamingStrategy )
					|| SingleNamingStrategy.STRATEGY_NAME.equals( idNamingStrategy )
					|| SingleNamingStrategy.class.getName().equals( idNamingStrategy ) ) {
				return 1;
			}
		}

		return OptimizableGenerator.DEFAULT_INCREMENT_SIZE;
	}

	static void collectBaselineProperties(
			Value identifierValue,
			Dialect dialect,
			RootClass rootClass,
			BiConsumer<String,String> parameterCollector,
			ConfigurationService configService) {
		//init the table here instead of earlier, so that we can get a quoted table name
		//TODO: would it be better to simply pass the qualified table name,
		//      instead of splitting it up into schema/catalog/table names
		parameterCollector.accept( TABLE, identifierValue.getTable().getQuotedName( dialect ) );

		//pass the column name (a generated id almost always has a single column)
		if ( identifierValue.getColumnSpan() == 1 ) {
			final var column = (Column) identifierValue.getSelectables().get( 0 );
			parameterCollector.accept( PK, column.getQuotedName( dialect ) );
		}

		//pass the entity-name, if not a collection-id
		if ( rootClass != null ) {
			parameterCollector.accept( ENTITY_NAME, rootClass.getEntityName() );
			parameterCollector.accept( JPA_ENTITY_NAME, rootClass.getJpaEntityName() );
			// The table name is not really a good default for subselect entities,
			// so use the JPA entity name which is short
			parameterCollector.accept(
					IMPLICIT_NAME_BASE,
					identifierValue.getTable().isSubselect()
							? rootClass.getJpaEntityName()
							: identifierValue.getTable().getName()
			);

			parameterCollector.accept( TABLES, identityTablesString( dialect, rootClass ) );
		}
		else {
			parameterCollector.accept( TABLES, identifierValue.getTable().getQuotedName( dialect ) );
			parameterCollector.accept( IMPLICIT_NAME_BASE, identifierValue.getTable().getQuotedName( dialect ) );
		}

		parameterCollector.accept( CONTRIBUTOR_NAME,
				identifierValue.getBuildingContext().getCurrentContributorName() );

		final var settings = configService.getSettings();
		if ( settings.containsKey( PREFERRED_POOLED_OPTIMIZER ) ) {
			parameterCollector.accept( PREFERRED_POOLED_OPTIMIZER,
					optimizerClassName( settings.get( PREFERRED_POOLED_OPTIMIZER ) ) );
		}
	}

	private static String optimizerClassName(Object optimizerSetting) {
		if  ( optimizerSetting instanceof String string ) {
			return string;
		}
		else if ( optimizerSetting instanceof Class<?> clazz ) {
			return clazz.getName();
		}
		else {
			return optimizerSetting.toString();
		}
	}

	public static String identityTablesString(Dialect dialect, RootClass rootClass) {
		final var tables = new StringBuilder();
		for ( var table : rootClass.getIdentityTables() ) {
			tables.append( table.getQuotedName( dialect ) );
			if ( !tables.isEmpty() ) {
				tables.append( ", " );
			}
		}
		return tables.toString();
	}

	public static int defaultIncrement(ConfigurationService configService) {
		final String idNamingStrategy =
				configService.getSetting( MappingSettings.ID_DB_STRUCTURE_NAMING_STRATEGY,
						StandardConverters.STRING, null );
		if ( LegacyNamingStrategy.STRATEGY_NAME.equals( idNamingStrategy )
				|| LegacyNamingStrategy.class.getName().equals( idNamingStrategy )
				|| SingleNamingStrategy.STRATEGY_NAME.equals( idNamingStrategy )
				|| SingleNamingStrategy.class.getName().equals( idNamingStrategy ) ) {
			return 1;
		}
		else {
			return OptimizableGenerator.DEFAULT_INCREMENT_SIZE;
		}
	}


	@Internal
	public static void interpretTableGenerator(
			TableGenerator tableGeneratorAnnotation,
			IdentifierGeneratorDefinition.Builder definitionBuilder) {
		definitionBuilder.setName( tableGeneratorAnnotation.name() );
		definitionBuilder.setStrategy( org.hibernate.id.enhanced.TableGenerator.class.getName() );
		definitionBuilder.addParam( CONFIG_PREFER_SEGMENT_PER_ENTITY, "true" );

		final String catalog = tableGeneratorAnnotation.catalog();
		if ( isNotBlank( catalog ) ) {
			definitionBuilder.addParam( CATALOG, catalog );
		}

		final String schema = tableGeneratorAnnotation.schema();
		if ( isNotBlank( schema ) ) {
			definitionBuilder.addParam( SCHEMA, schema );
		}

		final String table = tableGeneratorAnnotation.table();
		if ( isNotBlank( table ) ) {
			definitionBuilder.addParam( TABLE_PARAM, table );
		}

		final String pkColumnName = tableGeneratorAnnotation.pkColumnName();
		if ( isNotBlank( pkColumnName ) ) {
			definitionBuilder.addParam( SEGMENT_COLUMN_PARAM, pkColumnName );
		}

		final String pkColumnValue = tableGeneratorAnnotation.pkColumnValue();
		if ( isNotBlank( pkColumnValue ) ) {
			definitionBuilder.addParam( SEGMENT_VALUE_PARAM, pkColumnValue );
		}

		final String valueColumnName = tableGeneratorAnnotation.valueColumnName();
		if ( isNotBlank( valueColumnName ) ) {
			definitionBuilder.addParam( VALUE_COLUMN_PARAM, valueColumnName );
		}

		final String options = tableGeneratorAnnotation.options();
		if ( isNotBlank( options ) ) {
			definitionBuilder.addParam( OPTIONS, options );
		}

		definitionBuilder.addParam( INCREMENT_PARAM,
				String.valueOf( tableGeneratorAnnotation.allocationSize() ) );

		// See comment on HHH-4884 wrt initialValue. Basically initialValue is really the stated value + 1
		definitionBuilder.addParam( INITIAL_PARAM,
				String.valueOf( tableGeneratorAnnotation.initialValue() + 1 ) );

		// TODO: implement unique constraint support
		if ( isNotEmpty( tableGeneratorAnnotation.uniqueConstraints() ) ) {
			BOOT_LOGGER.ignoringTableGeneratorConstraints( tableGeneratorAnnotation.name() );
		}
	}

	@Internal
	public static void interpretSequenceGenerator(
			SequenceGenerator sequenceGeneratorAnnotation,
			IdentifierGeneratorDefinition.Builder definitionBuilder) {
		definitionBuilder.setName( sequenceGeneratorAnnotation.name() );
		definitionBuilder.setStrategy( SequenceStyleGenerator.class.getName() );

		final String catalog = sequenceGeneratorAnnotation.catalog();
		if ( isNotBlank( catalog ) ) {
			definitionBuilder.addParam( CATALOG, catalog );
		}

		final String schema = sequenceGeneratorAnnotation.schema();
		if ( isNotBlank( schema ) ) {
			definitionBuilder.addParam( SCHEMA, schema );
		}

		final String sequenceName = sequenceGeneratorAnnotation.sequenceName();
		if ( isNotBlank( sequenceName ) ) {
			definitionBuilder.addParam( SEQUENCE_PARAM, sequenceName );
		}

		definitionBuilder.addParam( INCREMENT_PARAM,
				String.valueOf( sequenceGeneratorAnnotation.allocationSize() ) );
		definitionBuilder.addParam( INITIAL_PARAM,
				String.valueOf( sequenceGeneratorAnnotation.initialValue() ) );

		final String options = sequenceGeneratorAnnotation.options();
		if ( isNotBlank( options ) ) {
			definitionBuilder.addParam( OPTIONS, options );
		}
	}
}
