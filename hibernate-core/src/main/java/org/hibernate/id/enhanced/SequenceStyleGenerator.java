/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.enhanced;

import java.util.Objects;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedNameParser;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
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
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

/**
 * Generates identifier values based on a sequence-style database structure.
 * Variations range from actually using a sequence to using a table to mimic
 * a sequence.  These variations are encapsulated by the {@link DatabaseStructure}
 * interface internally.
 * <p/>
 * General configuration parameters:
 * <table>
 * 	 <tr>
 *     <td><b>NAME</b></td>
 *     <td><b>DEFAULT</b></td>
 *     <td><b>DESCRIPTION</b></td>
 *   </tr>
 *   <tr>
 *     <td>{@link #SEQUENCE_PARAM}</td>
 *     <td>{@link #DEF_SEQUENCE_NAME}</td>
 *     <td>The name of the sequence/table to use to store/retrieve values</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #INITIAL_PARAM}</td>
 *     <td>{@link #DEFAULT_INITIAL_VALUE}</td>
 *     <td>The initial value to be stored for the given segment; the effect in terms of storage varies based on {@link Optimizer} and {@link DatabaseStructure}</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #INCREMENT_PARAM}</td>
 *     <td>{@link #DEFAULT_INCREMENT_SIZE}</td>
 *     <td>The increment size for the underlying segment; the effect in terms of storage varies based on {@link Optimizer} and {@link DatabaseStructure}</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #OPT_PARAM}</td>
 *     <td><i>depends on defined increment size</i></td>
 *     <td>Allows explicit definition of which optimization strategy to use</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #FORCE_TBL_PARAM}</td>
 *     <td><b><i>false</i></b></td>
 *     <td>Allows explicit definition of which optimization strategy to use</td>
 *   </tr>
 * </table>
 * <p/>
 * Configuration parameters used specifically when the underlying structure is a table:
 * <table>
 * 	 <tr>
 *     <td><b>NAME</b></td>
 *     <td><b>DEFAULT</b></td>
 *     <td><b>DESCRIPTION</b></td>
 *   </tr>
 *   <tr>
 *     <td>{@link #VALUE_COLUMN_PARAM}</td>
 *     <td>{@link #DEF_VALUE_COLUMN}</td>
 *     <td>The name of column which holds the sequence value for the given segment</td>
 *   </tr>
 * </table>
 *
 * @author Steve Ebersole
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
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

	/**
	 * @deprecated As of 6.0 with no replacement - `hibernate_sequence` as a real, implicit exportable name
	 * is no longer supported.  No effect
	 */
	@Deprecated
	public static final String DEF_SEQUENCE_NAME = "hibernate_sequence";

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
	 * Indicates the name of the column holding the identifier values.  The default value is {@link #DEF_VALUE_COLUMN}
	 */
	@SuppressWarnings("WeakerAccess")
	public static final String VALUE_COLUMN_PARAM = "value_column";

	/**
	 * The default value for {@link #VALUE_COLUMN_PARAM}
	 */
	@SuppressWarnings("WeakerAccess")
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
	public void configure(Type type, Properties params, ServiceRegistry serviceRegistry) throws MappingException {
		final JdbcEnvironment jdbcEnvironment = serviceRegistry.getService( JdbcEnvironment.class );
		final ConfigurationService configurationService = serviceRegistry.getService( ConfigurationService.class );

		final Dialect dialect = jdbcEnvironment.getDialect();

		this.identifierType = type;
		boolean forceTableUse = ConfigurationHelper.getBoolean( FORCE_TBL_PARAM, params, false );

		final QualifiedName sequenceName = determineSequenceName( params, dialect, jdbcEnvironment, serviceRegistry );

		final int initialValue = determineInitialValue( params );
		int incrementSize = determineIncrementSize( params );

		final String optimizationStrategy = determineOptimizationStrategy( params, incrementSize );

		final boolean isPooledOptimizer = OptimizerFactory.isPooledOptimizer( optimizationStrategy );


		SequenceMismatchStrategy sequenceMismatchStrategy = configurationService.getSetting(
				AvailableSettings.SEQUENCE_INCREMENT_SIZE_MISMATCH_STRATEGY,
				SequenceMismatchStrategy::interpret,
				SequenceMismatchStrategy.EXCEPTION
		);

		if ( sequenceMismatchStrategy != SequenceMismatchStrategy.NONE && isPooledOptimizer && isPhysicalSequence( jdbcEnvironment, forceTableUse ) ) {
			String databaseSequenceName = sequenceName.getObjectName().getText();
			Long databaseIncrementValue = getSequenceIncrementValue( jdbcEnvironment, databaseSequenceName );

			if ( databaseIncrementValue != null && !databaseIncrementValue.equals( (long) incrementSize ) ) {
				int dbIncrementValue = databaseIncrementValue.intValue();

				switch ( sequenceMismatchStrategy ) {
					case EXCEPTION:
						throw new MappingException(
								String.format(
										"The increment size of the [%s] sequence is set to [%d] in the entity mapping " +
												"while the associated database sequence increment size is [%d].",
										databaseSequenceName, incrementSize, dbIncrementValue
								)
						);
					case FIX:
						incrementSize = dbIncrementValue;
					case LOG:
						LOG.sequenceIncrementSizeMismatch( databaseSequenceName, incrementSize, dbIncrementValue );
						break;
				}
			}
		}

		incrementSize = determineAdjustedIncrementSize( optimizationStrategy, incrementSize );

		if ( dialect.getSequenceSupport().supportsSequences() && !forceTableUse ) {
			if ( !dialect.getSequenceSupport().supportsPooledSequences()
					&& OptimizerFactory.isPooledOptimizer( optimizationStrategy ) ) {
				forceTableUse = true;
				LOG.forcingTableUse();
			}
		}

		this.databaseStructure = buildDatabaseStructure(
				type,
				params,
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
				ConfigurationHelper.getInt( INITIAL_PARAM, params, -1 )
		);
		this.databaseStructure.configure( optimizer );
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
	 * <p/>
	 * Called during {@link #configure configuration}.
	 *
	 * @param params The params supplied in the generator config (plus some standard useful extras).
	 * @param dialect The dialect in effect
	 * @param jdbcEnv The JdbcEnvironment
	 * @return The sequence name
	 */
	@SuppressWarnings({"UnusedParameters", "WeakerAccess"})
	protected QualifiedName determineSequenceName(
			Properties params,
			Dialect dialect,
			JdbcEnvironment jdbcEnv,
			ServiceRegistry serviceRegistry) {
		final Identifier catalog = jdbcEnv.getIdentifierHelper().toIdentifier(
				ConfigurationHelper.getString( CATALOG, params )
		);
		final Identifier schema =  jdbcEnv.getIdentifierHelper().toIdentifier(
				ConfigurationHelper.getString( SCHEMA, params )
		);

		final String sequenceName = ConfigurationHelper.getString( SEQUENCE_PARAM, params );
		if ( StringHelper.isNotEmpty( sequenceName ) ) {
			// we have an explicit name, use it
			if ( sequenceName.contains( "." ) ) {
				//
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
		final String implicitName = determineImplicitName( params, jdbcEnv, serviceRegistry );
		return new QualifiedNameParser.NameParts(
				catalog,
				schema,
				jdbcEnv.getIdentifierHelper().toIdentifier( implicitName )
		);
	}

	private String determineImplicitName(Properties params, JdbcEnvironment jdbcEnv, ServiceRegistry serviceRegistry) {
		final String annotationGeneratorName = params.getProperty( IdentifierGenerator.GENERATOR_NAME );
		final String base = ConfigurationHelper.getString( IMPLICIT_NAME_BASE, params );
		final String suffix = ConfigurationHelper.getString( CONFIG_SEQUENCE_PER_ENTITY_SUFFIX, params, DEF_SEQUENCE_SUFFIX );

		if ( ! Objects.equals( suffix, DEF_SEQUENCE_SUFFIX ) ) {
			// an "implicit name suffix" was specified
			if ( StringHelper.isNotEmpty( base ) ) {
				if ( Identifier.isQuoted( base ) ) {
					return "`" + Identifier.unQuote( base ) + suffix + "`";
				}
				return base + suffix;
			}
		}

		if ( StringHelper.isNotEmpty( annotationGeneratorName ) ) {
			return annotationGeneratorName;
		}

		if ( StringHelper.isNotEmpty( base ) ) {
			if ( Identifier.isQuoted( base ) ) {
				return "`" + Identifier.unQuote( base ) + suffix + "`";
			}
			return base + suffix;
		}

		throw new MappingException( "Unable to determine sequence name" );
	}

	/**
	 * Determine the name of the column used to store the generator value in
	 * the db.
	 * <p/>
	 * Called during {@link #configure configuration} <b>when resolving to a
	 * physical table</b>.
	 *
	 * @param params The params supplied in the generator config (plus some standard useful extras).
	 * @param jdbcEnvironment The JDBC environment
	 * @return The value column name
	 */
	@SuppressWarnings({"UnusedParameters", "WeakerAccess"})
	protected Identifier determineValueColumnName(Properties params, JdbcEnvironment jdbcEnvironment) {
		final String name = ConfigurationHelper.getString( VALUE_COLUMN_PARAM, params, DEF_VALUE_COLUMN );
		return jdbcEnvironment.getIdentifierHelper().toIdentifier( name );
	}

	/**
	 * Determine the initial sequence value to use.  This value is used when
	 * initializing the {@link #getDatabaseStructure() database structure}
	 * (i.e. sequence/table).
	 * <p/>
	 * Called during {@link #configure configuration}.
	 *
	 * @param params The params supplied in the generator config (plus some standard useful extras).
	 * @return The initial value
	 */
	@SuppressWarnings({"WeakerAccess"})
	protected int determineInitialValue(Properties params) {
		return ConfigurationHelper.getInt( INITIAL_PARAM, params, DEFAULT_INITIAL_VALUE );
	}

	/**
	 * Determine the increment size to be applied.  The exact implications of
	 * this value depends on the {@link #getOptimizer() optimizer} being used.
	 * <p/>
	 * Called during {@link #configure configuration}.
	 *
	 * @param params The params supplied in the generator config (plus some standard useful extras).
	 * @return The increment size
	 */
	@SuppressWarnings("WeakerAccess")
	protected int determineIncrementSize(Properties params) {
		return ConfigurationHelper.getInt( INCREMENT_PARAM, params, DEFAULT_INCREMENT_SIZE );
	}

	/**
	 * Determine the optimizer to use.
	 * <p/>
	 * Called during {@link #configure configuration}.
	 *
	 * @param params The params supplied in the generator config (plus some standard useful extras).
	 * @param incrementSize The {@link #determineIncrementSize determined increment size}
	 * @return The optimizer strategy (name)
	 */
	@SuppressWarnings("WeakerAccess")
	protected String determineOptimizationStrategy(Properties params, int incrementSize) {
		return ConfigurationHelper.getString(
				OPT_PARAM,
				params,
				OptimizerFactory.determineImplicitOptimizerName( incrementSize, params )
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
	@SuppressWarnings("WeakerAccess")
	protected int determineAdjustedIncrementSize(String optimizationStrategy, int incrementSize) {
		final int resolvedIncrementSize;
		if ( Math.abs( incrementSize ) > 1 &&
				StandardOptimizerDescriptor.NONE.getExternalName().equals( optimizationStrategy ) ) {
			if ( incrementSize < -1 ) {
				resolvedIncrementSize = -1;
				LOG.honoringOptimizerSetting(
						StandardOptimizerDescriptor.NONE.getExternalName(),
						INCREMENT_PARAM,
						incrementSize,
						"negative",
						resolvedIncrementSize
				);
			}
			else {
				// incrementSize > 1
				resolvedIncrementSize = 1;
				LOG.honoringOptimizerSetting(
						StandardOptimizerDescriptor.NONE.getExternalName(),
						INCREMENT_PARAM,
						incrementSize,
						"positive",
						resolvedIncrementSize
				);
			}
		}
		else {
			resolvedIncrementSize = incrementSize;
		}
		return resolvedIncrementSize;
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
	@SuppressWarnings("WeakerAccess")
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

	@SuppressWarnings("WeakerAccess")
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
		return context.getDialect().getSequenceSupport().getSelectSequenceNextValString( context.format( getDatabaseStructure().getPhysicalName() ) );
	}

	/**
	 * Get the database sequence increment value from the associated {@link SequenceInformation} object.
	 *
	 * @param jdbcEnvironment the current JdbcEnvironment
	 * @param sequenceName sequence name
	 *
	 * @return sequence increment value
	 */
	private Long getSequenceIncrementValue(JdbcEnvironment jdbcEnvironment, String sequenceName) {
		return jdbcEnvironment.getExtractedDatabaseMetaData().getSequenceInformationList()
				.stream()
				.filter(
					sequenceInformation -> {
						Identifier catalog = sequenceInformation.getSequenceName().getCatalogName();
						Identifier schema = sequenceInformation.getSequenceName().getSchemaName();
						return sequenceName.equalsIgnoreCase( sequenceInformation.getSequenceName().getSequenceName().getText() ) &&
								( catalog == null || catalog.equals( jdbcEnvironment.getCurrentCatalog() ) ) &&
								( schema == null || schema.equals( jdbcEnvironment.getCurrentSchema() ) );
					}
				)
				.map( SequenceInformation::getIncrementValue )
				.filter( Objects::nonNull )
				.findFirst()
				.orElse( null );
	}
}
