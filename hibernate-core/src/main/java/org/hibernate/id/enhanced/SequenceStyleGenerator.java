/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.enhanced;

import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedNameParser;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.BulkInsertionCapableIdentifierGenerator;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.SequenceMismatchStrategy;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.Action;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator.ActionGrouping;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

import static org.hibernate.cfg.AvailableSettings.ID_DB_STRUCTURE_NAMING_STRATEGY;
import static org.hibernate.id.enhanced.OptimizerFactory.determineImplicitOptimizerName;
import static org.hibernate.internal.log.IncubationLogger.INCUBATION_LOGGER;
import static org.hibernate.internal.util.NullnessHelper.coalesceSuppliedValues;
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
 *     <td>The name of column which holds the sequence value for the given segment</td>
 *   </tr>
 * </table>
 *
 * @author Steve Ebersole
 * @author Lukasz Antoniak
 */
public class SequenceStyleGenerator
		implements PersistentIdentifierGenerator, BulkInsertionCapableIdentifierGenerator {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			SequenceStyleGenerator.class.getName()
	);


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
	public void configure(Type type, Properties parameters, ServiceRegistry serviceRegistry) throws MappingException {
		final JdbcEnvironment jdbcEnvironment = serviceRegistry.requireService( JdbcEnvironment.class );
		final Dialect dialect = jdbcEnvironment.getDialect();

		this.identifierType = type;

		final QualifiedName sequenceName = determineSequenceName( parameters, dialect, jdbcEnvironment, serviceRegistry );
		final int initialValue = determineInitialValue( parameters );
		int incrementSize = determineIncrementSize( parameters );
		final OptimizerDescriptor optimizationStrategy = determineOptimizationStrategy( parameters, incrementSize );

		boolean forceTableUse = getBoolean( FORCE_TBL_PARAM, parameters );
		final boolean physicalSequence = isPhysicalSequence( jdbcEnvironment, forceTableUse );

		incrementSize = adjustIncrementSize(
				jdbcEnvironment,
				sequenceName,
				incrementSize,
				physicalSequence,
				optimizationStrategy,
				serviceRegistry,
				determineContributor( parameters )
		);

		if ( physicalSequence
				&& optimizationStrategy.isPooled()
				&& !dialect.getSequenceSupport().supportsPooledSequences() ) {
			forceTableUse = true;
			LOG.forcingTableUse();
		}

		this.databaseStructure = buildDatabaseStructure(
				type,
				parameters,
				jdbcEnvironment,
				forceTableUse,
				sequenceName,
				initialValue,
				incrementSize
		);
		this.optimizer = OptimizerFactory.buildOptimizer(
				optimizationStrategy,
				identifierType.getReturnedClass(),
				incrementSize,
				getInt( INITIAL_PARAM, parameters, -1 )
		);
		this.databaseStructure.configure( optimizer );
	}

	private int adjustIncrementSize(
			JdbcEnvironment jdbcEnvironment,
			QualifiedName sequenceName,
			int incrementSize,
			boolean physicalSequence,
			OptimizerDescriptor optimizationStrategy,
			ServiceRegistry serviceRegistry,
			String contributor) {
		final ConfigurationService configurationService = serviceRegistry.requireService( ConfigurationService.class );
		final SequenceMismatchStrategy sequenceMismatchStrategy = configurationService.getSetting(
				AvailableSettings.SEQUENCE_INCREMENT_SIZE_MISMATCH_STRATEGY,
				SequenceMismatchStrategy::interpret,
				SequenceMismatchStrategy.EXCEPTION
		);

		if ( sequenceMismatchStrategy != SequenceMismatchStrategy.NONE
				&& optimizationStrategy.isPooled()
				&& physicalSequence ) {
			final String databaseSequenceName = sequenceName.getObjectName().getText();
			final Number databaseIncrementValue = isSchemaToBeRecreated( contributor, configurationService ) ? null : getSequenceIncrementValue( jdbcEnvironment, databaseSequenceName );
			if ( databaseIncrementValue != null && databaseIncrementValue.intValue() != incrementSize) {
				final int dbIncrementValue = databaseIncrementValue.intValue();
				switch ( sequenceMismatchStrategy ) {
					case EXCEPTION:
						throw new MappingException(
								String.format(
										"The increment size of the [%s] sequence is set to [%d] in the entity mapping "
												+ "while the associated database sequence increment size is [%d].",
										databaseSequenceName, incrementSize, dbIncrementValue
								)
						);
					case FIX:
						incrementSize = dbIncrementValue;
					case LOG:
						//TODO: the log message is correct for the case of FIX, but wrong for LOG
						LOG.sequenceIncrementSizeMismatch( databaseSequenceName, incrementSize, dbIncrementValue );
						break;
				}
			}
		}
		return determineAdjustedIncrementSize( optimizationStrategy, incrementSize );
	}

	private boolean isSchemaToBeRecreated(String contributor, ConfigurationService configurationService) {
		final Set<ActionGrouping> actions = ActionGrouping.interpret( Collections.singleton(contributor), configurationService.getSettings() );
		// We know this will only contain at most 1 action
		final Iterator<ActionGrouping> it = actions.iterator();
		final Action dbAction = it.hasNext() ? it.next().getDatabaseAction() : null;
		return dbAction == Action.CREATE || dbAction == Action.CREATE_DROP;
	}

	@Override
	public void registerExportables(Database database) {
		databaseStructure.registerExportables( database );
	}

	@Override
	public void initialize(SqlStringGenerationContext context) {
		this.databaseStructure.initialize( context );
	}

	/**
	 * Determine the name of the sequence (or table if this resolves to a physical table)
	 * to use.
	 * <p>
	 * Called during {@linkplain #configure configuration}.
	 *
	 * @param params The params supplied in the generator config (plus some standard useful extras).
	 * @param dialect The dialect in effect
	 * @param jdbcEnv The JdbcEnvironment
	 * @return The sequence name
	 */
	@SuppressWarnings("UnusedParameters")
	protected QualifiedName determineSequenceName(
			Properties params,
			Dialect dialect,
			JdbcEnvironment jdbcEnv,
			ServiceRegistry serviceRegistry) {
		final Identifier catalog = jdbcEnv.getIdentifierHelper().toIdentifier(
				getString( CATALOG, params )
		);
		final Identifier schema =  jdbcEnv.getIdentifierHelper().toIdentifier(
				getString( SCHEMA, params )
		);

		final String sequenceName = getString(
				SEQUENCE_PARAM,
				params,
				() -> getString( ALT_SEQUENCE_PARAM, params )
		);

		if ( StringHelper.isNotEmpty( sequenceName ) ) {
			// we have an explicit name, use it
			if ( sequenceName.contains( "." ) ) {
				return QualifiedNameParser.INSTANCE.parse( sequenceName );
			}
			else {
				return new QualifiedNameParser.NameParts(
						catalog,
						schema,
						jdbcEnv.getIdentifierHelper().toIdentifier( sequenceName )
				);
			}
		}

		// otherwise, determine an implicit name to use
		return determineImplicitName( catalog, schema, params, serviceRegistry );
	}

	private QualifiedName determineImplicitName(
			Identifier catalog,
			Identifier schema,
			Properties params,
			ServiceRegistry serviceRegistry) {
		final StrategySelector strategySelector = serviceRegistry.requireService( StrategySelector.class );

		final String namingStrategySetting = coalesceSuppliedValues(
				() -> {
					final String localSetting = getString( ID_DB_STRUCTURE_NAMING_STRATEGY, params );
					if ( localSetting != null ) {
						INCUBATION_LOGGER.incubatingSetting( ID_DB_STRUCTURE_NAMING_STRATEGY );
					}
					return localSetting;
				},
				() -> {
					final ConfigurationService configurationService = serviceRegistry.requireService( ConfigurationService.class );
					final String globalSetting = getString( ID_DB_STRUCTURE_NAMING_STRATEGY, configurationService.getSettings() );
					if ( globalSetting != null ) {
						INCUBATION_LOGGER.incubatingSetting( ID_DB_STRUCTURE_NAMING_STRATEGY );
					}
					return globalSetting;
				},
				StandardNamingStrategy.class::getName
		);

		final ImplicitDatabaseObjectNamingStrategy namingStrategy = strategySelector.resolveStrategy(
				ImplicitDatabaseObjectNamingStrategy.class,
				namingStrategySetting
		);

		return namingStrategy.determineSequenceName( catalog, schema, params, serviceRegistry );
	}

	/**
	 * Determine the name of the column used to store the generator value in
	 * the db.
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
	 * Determine the increment size to be applied.  The exact implications of
	 * this value depends on the {@linkplain #getOptimizer() optimizer} being used.
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
	 * selected optimizer.  This is the hook to achieve that.
	 *
	 * @param optimizationStrategy The optimizer strategy (name)
	 * @param incrementSize The {@link #determineIncrementSize determined increment size}
	 * @return The adjusted increment size.
	 */
	protected int determineAdjustedIncrementSize(OptimizerDescriptor optimizationStrategy, int incrementSize) {
		if ( optimizationStrategy == StandardOptimizerDescriptor.NONE  ) {
			if ( incrementSize < -1 ) {
				LOG.honoringOptimizerSetting(
						StandardOptimizerDescriptor.NONE.getExternalName(),
						INCREMENT_PARAM,
						incrementSize,
						"negative",
						-1
				);
				return -1;
			}
			else if ( incrementSize > 1 ) {
				LOG.honoringOptimizerSetting(
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
		if ( isPhysicalSequence( jdbcEnvironment, forceTableUse ) ) {
			return buildSequenceStructure( type, params, jdbcEnvironment, sequenceName, initialValue, incrementSize );
		}
		else {
			return buildTableStructure( type, params, jdbcEnvironment, sequenceName, initialValue, incrementSize );
		}
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
				jdbcEnvironment,
				determineContributor( params ),
				sequenceName,
				initialValue,
				incrementSize,
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
		final Identifier valueColumnName = determineValueColumnName( params, jdbcEnvironment );
		final String contributor = determineContributor( params );

		return new TableStructure(
				jdbcEnvironment,
				contributor,
				sequenceName,
				valueColumnName,
				initialValue,
				incrementSize,
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
		return jdbcEnvironment.getExtractedDatabaseMetaData().getSequenceInformationList()
				.stream()
				.filter(
					sequenceInformation -> {
						QualifiedSequenceName name = sequenceInformation.getSequenceName();
						Identifier catalog = name.getCatalogName();
						Identifier schema = name.getSchemaName();
						return sequenceName.equalsIgnoreCase( name.getSequenceName().getText() )
							&& ( catalog == null || catalog.equals( jdbcEnvironment.getCurrentCatalog() ) )
							&& ( schema == null || schema.equals( jdbcEnvironment.getCurrentSchema() ) );
					}
				)
				.map( SequenceInformation::getIncrementValue )
				.filter( Objects::nonNull )
				.findFirst()
				.orElse( null );
	}
}
