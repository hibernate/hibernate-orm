/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.enhanced;

import java.util.Properties;
import java.util.function.BiConsumer;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedNameParser;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.id.BulkInsertionCapableIdentifierGenerator;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.SequenceMismatchStrategy;
import org.hibernate.mapping.Table;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.Action;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator.ActionGrouping;
import org.hibernate.type.Type;


import jakarta.persistence.SequenceGenerator;

import static java.util.Collections.singleton;
import static org.hibernate.cfg.MappingSettings.SEQUENCE_INCREMENT_SIZE_MISMATCH_STRATEGY;
import static org.hibernate.id.IdentifierGeneratorHelper.getNamingStrategy;
import static org.hibernate.id.enhanced.OptimizerFactory.determineImplicitOptimizerName;
import static org.hibernate.id.enhanced.SequenceGeneratorLogger.SEQUENCE_GENERATOR_LOGGER;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.config.ConfigurationHelper.getBoolean;
import static org.hibernate.internal.util.config.ConfigurationHelper.getInt;
import static org.hibernate.internal.util.config.ConfigurationHelper.getString;

/**
 * Generates identifier values based on a sequence-style database structure.
 * Variations range from actually using a sequence to using a table to mimic
 * a sequence.  These variations are encapsulated by the {@link DatabaseStructure}
 * interface internally.
 * <p>
 * <table>
 * <caption>General configuration parameters</caption>
 * 	 <tr>
 *     <td><b>Parameter name</b></td>
 *     <td><b>Default value</b></td>
 *     <td><b>Interpretation</b></td>
 *   </tr>
 *   <tr>
 *     <td>{@value #SEQUENCE_PARAM}</td>
 *     <td></td>
 *     <td>The name of the sequence/table to use to store/retrieve values</td>
 *   </tr>
 *   <tr>
 *     <td>{@value #INITIAL_PARAM}</td>
 *     <td>{@value #DEFAULT_INITIAL_VALUE}</td>
 *     <td>The initial value to be stored for the given segment;
 *         the effect in terms of storage varies based on {@link Optimizer}
 *         and {@link DatabaseStructure}</td>
 *   </tr>
 *   <tr>
 *     <td>{@value #INCREMENT_PARAM}</td>
 *     <td>{@value #DEFAULT_INCREMENT_SIZE}</td>
 *     <td>The increment size for the underlying segment;
 *         the effect in terms of storage varies based on {@link Optimizer}
 *         and {@link DatabaseStructure}</td>
 *   </tr>
 *   <tr>
 *     <td>{@value #OPT_PARAM}</td>
 *     <td><em>depends on defined increment size</em></td>
 *     <td>Allows explicit definition of which optimization strategy to use</td>
 *   </tr>
 *   <tr>
 *     <td>{@value #FORCE_TBL_PARAM}</td>
 *     <td>{@code false}</td>
 *     <td>Allows explicit definition of which optimization strategy to use</td>
 *   </tr>
 * </table>
 * <p>
 * Configuration parameters used specifically when the underlying structure is a table:
 * <table>
 * <caption>Table configuration parameters</caption>
 * 	 <tr>
 *     <td><b>Parameter name</b></td>
 *     <td><b>Default value</b></td>
 *     <td><b>Interpretation</b></td>
 *   </tr>
 *   <tr>
 *     <td>{@value #VALUE_COLUMN_PARAM}</td>
 *     <td>{@value #DEF_VALUE_COLUMN}</td>
 *     <td>The name of the column which holds the sequence value for the given segment</td>
 *   </tr>
 * </table>
 *
 * @author Steve Ebersole
 * @author Lukasz Antoniak
 */
public class SequenceStyleGenerator
		implements PersistentIdentifierGenerator, BulkInsertionCapableIdentifierGenerator, BeforeExecutionGenerator {

	// general purpose parameters ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Indicates the name of the sequence (or table) to use.  The implicit value is
	 * based on the entity / collection-role name
	 */
	public static final String SEQUENCE_PARAM = "sequence_name";
	public static final String ALT_SEQUENCE_PARAM = "sequence";

	/**
	 * Specifies the suffix to use for an implicit sequence name - appended to the entity-name / collection-role
	 */
	public static final String CONFIG_SEQUENCE_PER_ENTITY_SUFFIX = "sequence_per_entity_suffix";

	/**
	 * The default value for {@link #CONFIG_SEQUENCE_PER_ENTITY_SUFFIX}
	 */
	public static final String DEF_SEQUENCE_SUFFIX = "_SEQ";

	/**
	 * A flag to force using a table as the underlying structure rather than a sequence.
	 */
	public static final String FORCE_TBL_PARAM = "force_table_use";


	// table-specific parameters ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Indicates the name of the column holding the identifier values.
	 * The default value is {@value #DEF_VALUE_COLUMN}
	 */
	public static final String VALUE_COLUMN_PARAM = "value_column";

	/**
	 * The default value for {@link #VALUE_COLUMN_PARAM}
	 */
	public static final String DEF_VALUE_COLUMN = "next_val";


	// state ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private DatabaseStructure databaseStructure;
	private Optimizer optimizer;
	private Type identifierType;
	private Table table;

	/**
	 * Getter for property 'databaseStructure'.
	 *
	 * @return Value for property 'databaseStructure'.
	 */
	public DatabaseStructure getDatabaseStructure() {
		return databaseStructure;
	}

	/**
	 * Getter for property 'optimizer'.
	 *
	 * @return Value for property 'optimizer'.
	 */
	@Override
	public Optimizer getOptimizer() {
		return optimizer;
	}

	/**
	 * Getter for property 'identifierType'.
	 *
	 * @return Value for property 'identifierType'.
	 */
	public Type getIdentifierType() {
		return identifierType;
	}


	// Configurable implementation ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void configure(GeneratorCreationContext creationContext, Properties parameters) throws MappingException {
		final var serviceRegistry = creationContext.getServiceRegistry();
		final var jdbcEnvironment = serviceRegistry.requireService( JdbcEnvironment.class );
		final var dialect = jdbcEnvironment.getDialect();

		identifierType = creationContext.getType();
		table = creationContext.getValue().getTable();

		final var sequenceName = determineSequenceName( parameters, jdbcEnvironment, serviceRegistry );
		final int initialValue = determineInitialValue( parameters );
		int incrementSize = determineIncrementSize( parameters );
		final var optimizationStrategy = determineOptimizationStrategy( parameters, incrementSize );

		boolean forceTableUse = getBoolean( FORCE_TBL_PARAM, parameters );
		final boolean physicalSequence = isPhysicalSequence( jdbcEnvironment, forceTableUse );

		incrementSize = adjustIncrementSize(
				jdbcEnvironment,
				sequenceName,
				incrementSize,
				physicalSequence,
				optimizationStrategy,
				serviceRegistry,
				determineContributor( parameters ),
				creationContext
		);

		if ( physicalSequence
				&& optimizationStrategy.isPooled()
				&& !dialect.getSequenceSupport().supportsPooledSequences() ) {
			forceTableUse = true;
			SEQUENCE_GENERATOR_LOGGER.forcingTableUse();
		}

		databaseStructure = buildDatabaseStructure(
				identifierType,
				parameters,
				jdbcEnvironment,
				forceTableUse,
				sequenceName,
				initialValue,
				incrementSize
		);
		optimizer = OptimizerFactory.buildOptimizer(
				optimizationStrategy,
				identifierType.getReturnedClass(),
				incrementSize,
				getInt( INITIAL_PARAM, parameters, -1 )
		);
		databaseStructure.configure( optimizer );
	}

	private int adjustIncrementSize(
			JdbcEnvironment jdbcEnvironment,
			QualifiedName sequenceName,
			int incrementSize,
			boolean physicalSequence,
			OptimizerDescriptor optimizationStrategy,
			ServiceRegistry serviceRegistry,
			String contributor,
			GeneratorCreationContext creationContext) {
		final var configurationService = serviceRegistry.requireService( ConfigurationService.class );
		final var sequenceMismatchStrategy = configurationService.getSetting(
				SEQUENCE_INCREMENT_SIZE_MISMATCH_STRATEGY,
				SequenceMismatchStrategy::interpret,
				SequenceMismatchStrategy.EXCEPTION
		);

		if ( sequenceMismatchStrategy != SequenceMismatchStrategy.NONE
				&& optimizationStrategy.isPooled()
				&& physicalSequence ) {
			incrementSize =
					validatedIncrementSize(
							jdbcEnvironment,
							sequenceName,
							incrementSize,
							contributor,
							creationContext,
							configurationService,
							sequenceMismatchStrategy
					);
		}
		return determineAdjustedIncrementSize( optimizationStrategy, incrementSize );
	}

	private int validatedIncrementSize(
			JdbcEnvironment jdbcEnvironment,
			QualifiedName sequenceName,
			int incrementSize,
			String contributor,
			GeneratorCreationContext creationContext,
			ConfigurationService configurationService,
			SequenceMismatchStrategy sequenceMismatchStrategy) {
		final var database = creationContext.getDatabase();
		final Identifier databaseSequenceIdentifier =
				database != null
						? database.getPhysicalNamingStrategy()
								.toPhysicalSequenceName( sequenceName.getObjectName(), jdbcEnvironment )
						: sequenceName.getObjectName();
		final String databaseSequenceName = databaseSequenceIdentifier.getText();
		final Number databaseIncrementValue =
				isSchemaToBeRecreated( contributor, configurationService ) ? null
						: getSequenceIncrementValue( jdbcEnvironment, databaseSequenceName );
		if ( databaseIncrementValue != null && databaseIncrementValue.intValue() != incrementSize ) {
			final int dbIncrementValue = databaseIncrementValue.intValue();
			return switch ( sequenceMismatchStrategy ) {
				case NONE -> incrementSize;
				case FIX -> {
					// log at TRACE level
					SEQUENCE_GENERATOR_LOGGER.sequenceIncrementSizeMismatchFixed(
							databaseSequenceName, incrementSize, dbIncrementValue );
					yield dbIncrementValue;
				}
				case LOG -> {
					// log at WARN level
					SEQUENCE_GENERATOR_LOGGER.sequenceIncrementSizeMismatch(
							databaseSequenceName, incrementSize, dbIncrementValue );
					yield incrementSize;
				}
				case EXCEPTION -> throw new MappingException(
						String.format(
								"The increment size of the [%s] sequence is set to [%d] in the entity mapping "
									+ "but the mapped database sequence increment size is [%d]",
								databaseSequenceName, incrementSize, dbIncrementValue
						)
				);
			};
		}
		else {
			return incrementSize;
		}
	}

	private boolean isSchemaToBeRecreated(String contributor, ConfigurationService configurationService) {
		final var actions =
				ActionGrouping.interpret( singleton( contributor ),
						configurationService.getSettings() );
		// We know this will only contain at most 1 action
		final var iterator = actions.iterator();
		final var action = iterator.hasNext() ? iterator.next().databaseAction() : null;
		return action == Action.CREATE || action == Action.CREATE_DROP;
	}

	@Override
	public void registerExportables(Database database) {
		databaseStructure.registerExportables( database );
		databaseStructure.registerExtraExportables( table, optimizer );
	}

	@Override
	public void initialize(SqlStringGenerationContext context) {
		databaseStructure.initialize( context );
	}

	/**
	 * Determine the name of the sequence (or table if this resolves to a physical table)
	 * to use.
	 * <p>
	 * Called during {@linkplain #configure configuration}.
	 *
	 * @param params  The params supplied in the generator config (plus some standard useful extras).
	 * @param jdbcEnv The JdbcEnvironment
	 * @return The sequence name
	 */
	protected QualifiedName determineSequenceName(
			Properties params,
			JdbcEnvironment jdbcEnv,
			ServiceRegistry serviceRegistry) {
		final var identifierHelper = jdbcEnv.getIdentifierHelper();
		final Identifier catalog = identifierHelper.toIdentifier( getString( CATALOG, params ) );
		final Identifier schema =  identifierHelper.toIdentifier( getString( SCHEMA, params ) );
		final String sequenceName = getString( SEQUENCE_PARAM, params, () -> getString( ALT_SEQUENCE_PARAM, params ) );
		return sequenceName( params, serviceRegistry, sequenceName, catalog, schema, identifierHelper );
	}

	private static QualifiedName sequenceName(
			Properties params,
			ServiceRegistry serviceRegistry,
			String explicitSequenceName,
			Identifier catalog, Identifier schema,
			IdentifierHelper identifierHelper) {
		if ( isNotEmpty( explicitSequenceName ) ) {
			// we have an explicit name, use it
			return explicitSequenceName.contains(".")
					? QualifiedNameParser.INSTANCE.parse( explicitSequenceName )
					: new QualifiedNameParser.NameParts( catalog, schema,
							identifierHelper.toIdentifier( explicitSequenceName ) );
		}
		else {
			// otherwise, determine an implicit name to use
			return getNamingStrategy( params, serviceRegistry )
					.determineSequenceName( catalog, schema, params, serviceRegistry );
		}
	}


	/**
	 * Determine the name of the column used to store the generator value in
	 * the database.
	 * <p>
	 * Called during {@linkplain #configure configuration} <b>when resolving to a
	 * physical table</b>.
	 *
	 * @param params The params supplied in the generator config (plus some standard useful extras).
	 * @param jdbcEnvironment The JDBC environment
	 * @return The value column name
	 */
	protected Identifier determineValueColumnName(Properties params, JdbcEnvironment jdbcEnvironment) {
		final String name = getString( VALUE_COLUMN_PARAM, params, DEF_VALUE_COLUMN );
		return jdbcEnvironment.getIdentifierHelper().toIdentifier( name );
	}

	/**
	 * Determine the initial sequence value to use.  This value is used when
	 * initializing the {@link #getDatabaseStructure() database structure}
	 * (i.e. sequence/table).
	 * <p>
	 * Called during {@linkplain #configure configuration}.
	 *
	 * @param params The params supplied in the generator config (plus some standard useful extras).
	 * @return The initial value
	 */
	protected int determineInitialValue(Properties params) {
		return getInt( INITIAL_PARAM, params, DEFAULT_INITIAL_VALUE );
	}

	/**
	 * Determine the increment size to be applied. The exact implications of
	 * this value depend on the {@linkplain #getOptimizer() optimizer} in use.
	 * <p>
	 * Called during {@linkplain #configure configuration}.
	 *
	 * @param params The params supplied in the generator config (plus some standard useful extras).
	 * @return The increment size
	 */
	protected int determineIncrementSize(Properties params) {
		return getInt( INCREMENT_PARAM, params, DEFAULT_INCREMENT_SIZE );
	}

	/**
	 * Determine the optimizer to use.
	 * <p>
	 * Called during {@linkplain #configure configuration}.
	 *
	 * @param params        The params supplied in the generator config (plus some standard useful extras).
	 * @param incrementSize The {@link #determineIncrementSize determined increment size}
	 * @return The optimizer strategy (name)
	 */
	protected OptimizerDescriptor determineOptimizationStrategy(Properties params, int incrementSize) {
		return StandardOptimizerDescriptor.fromExternalName(
				getString( OPT_PARAM, params, determineImplicitOptimizerName( incrementSize, params ) )
		);
	}

	/**
	 * In certain cases we need to adjust the increment size based on the
	 * selected optimizer. This is the hook to achieve that.
	 *
	 * @param optimizationStrategy The optimizer strategy (name)
	 * @param incrementSize The {@link #determineIncrementSize determined increment size}
	 * @return The adjusted increment size.
	 */
	protected int determineAdjustedIncrementSize(OptimizerDescriptor optimizationStrategy, int incrementSize) {
		if ( optimizationStrategy == StandardOptimizerDescriptor.NONE  ) {
			if ( incrementSize < -1 ) {
				SEQUENCE_GENERATOR_LOGGER.honoringOptimizerSetting(
						StandardOptimizerDescriptor.NONE.getExternalName(),
						INCREMENT_PARAM,
						incrementSize,
						"negative",
						-1
				);
				return -1;
			}
			else if ( incrementSize > 1 ) {
				SEQUENCE_GENERATOR_LOGGER.honoringOptimizerSetting(
						StandardOptimizerDescriptor.NONE.getExternalName(),
						INCREMENT_PARAM,
						incrementSize,
						"positive",
						1
				);
				return 1;
			}
			else {
				return incrementSize;
			}
		}
		else {
			return incrementSize;
		}
	}

	/**
	 * Build the database structure.
	 *
	 * @param type The Hibernate type of the identifier property
	 * @param params The params supplied in the generator config (plus some standard useful extras).
	 * @param jdbcEnvironment The JDBC environment in which the sequence will be used.
	 * @param forceTableUse Should a table be used even if the dialect supports sequences?
	 * @param sequenceName The name to use for the sequence or table.
	 * @param initialValue The initial value.
	 * @param incrementSize the increment size to use (after any adjustments).
	 *
	 * @return An abstraction for the actual database structure in use (table vs. sequence).
	 */
	protected DatabaseStructure buildDatabaseStructure(
			Type type,
			Properties params,
			JdbcEnvironment jdbcEnvironment,
			boolean forceTableUse,
			QualifiedName sequenceName,
			int initialValue,
			int incrementSize) {
		return isPhysicalSequence( jdbcEnvironment, forceTableUse )
				? buildSequenceStructure( type, params, jdbcEnvironment, sequenceName, initialValue, incrementSize )
				: buildTableStructure( type, params, jdbcEnvironment, sequenceName, initialValue, incrementSize );
	}

	protected boolean isPhysicalSequence(JdbcEnvironment jdbcEnvironment, boolean forceTableUse) {
		return jdbcEnvironment.getDialect().getSequenceSupport().supportsSequences()
			&& !forceTableUse;
	}

	protected DatabaseStructure buildSequenceStructure(
			Type type,
			Properties params,
			JdbcEnvironment jdbcEnvironment,
			QualifiedName sequenceName,
			int initialValue,
			int incrementSize) {
		return new SequenceStructure(
				determineContributor( params ),
				sequenceName,
				initialValue,
				incrementSize,
				params.getProperty( OPTIONS ),
				type.getReturnedClass()
		);
	}

	protected DatabaseStructure buildTableStructure(
			Type type,
			Properties params,
			JdbcEnvironment jdbcEnvironment,
			QualifiedName sequenceName,
			int initialValue,
			int incrementSize) {

		return new TableStructure(
				determineContributor( params ),
				sequenceName,
				determineValueColumnName( params, jdbcEnvironment ),
				initialValue,
				incrementSize,
				params.getProperty( OPTIONS ),
				type.getReturnedClass()
		);
	}

	private String determineContributor(Properties params) {
		final String contributor = params.getProperty( IdentifierGenerator.CONTRIBUTOR_NAME );
		return contributor == null ? "orm" : contributor;
	}


	// IdentifierGenerator implementation ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public Object generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
		return optimizer.generate( databaseStructure.buildCallback( session ) );
	}

	// BulkInsertionCapableIdentifierGenerator implementation ~~~~~~~~~~~~~~~~~

	@Override
	public boolean supportsBulkInsertionIdentifierGeneration() {
		// it does, as long as the underlying structure is a sequence
		return getDatabaseStructure().isPhysicalSequence();
	}

	@Override
	public String determineBulkInsertionIdentifierGenerationSelectFragment(SqlStringGenerationContext context) {
		return context.getDialect().getSequenceSupport()
				.getSelectSequenceNextValString( context.format( getDatabaseStructure().getPhysicalName() ) );
	}

	/**
	 * Get the database sequence increment value from the associated {@link SequenceInformation} object.
	 *
	 * @param jdbcEnvironment the current JdbcEnvironment
	 * @param sequenceName sequence name
	 *
	 * @return sequence increment value
	 */
	private Number getSequenceIncrementValue(JdbcEnvironment jdbcEnvironment, String sequenceName) {
		for ( var information : jdbcEnvironment.getExtractedDatabaseMetaData().getSequenceInformationList() ) {
			final var name = information.getSequenceName();
			if ( sequenceName.equalsIgnoreCase( name.getSequenceName().getText() )
					&& isDefaultSchema( jdbcEnvironment, name.getCatalogName(), name.getSchemaName() ) ) {
				final Number incrementValue = information.getIncrementValue();
				if ( incrementValue != null ) {
					return incrementValue;
				}
			}
		}
		return null;
	}

	private static boolean isDefaultSchema(JdbcEnvironment jdbcEnvironment, Identifier catalog, Identifier schema) {
		return ( catalog == null || catalog.equals( jdbcEnvironment.getCurrentCatalog() ) )
			&& ( schema == null || schema.equals( jdbcEnvironment.getCurrentSchema() ) );
	}

	public static void applyConfiguration(SequenceGenerator generatorConfig, BiConsumer<String,String> configCollector) {
		if ( !generatorConfig.sequenceName().isEmpty() ) {
			configCollector.accept( SEQUENCE_PARAM, generatorConfig.sequenceName() );
		}
		if ( !generatorConfig.catalog().isEmpty() ) {
			configCollector.accept( CATALOG, generatorConfig.catalog() );
		}
		if ( !generatorConfig.schema().isEmpty() ) {
			configCollector.accept( SCHEMA, generatorConfig.schema() );
		}
		if ( !generatorConfig.options().isEmpty() ) {
			configCollector.accept( OPTIONS, generatorConfig.options() );
		}

		configCollector.accept( INITIAL_PARAM, Integer.toString( generatorConfig.initialValue() ) );
		if ( generatorConfig.allocationSize() == 50 ) {
			// don't do anything - assuming a proper default is already set
		}
		else {
			configCollector.accept( INCREMENT_PARAM, Integer.toString( generatorConfig.allocationSize() ) );
		}
	}
}
