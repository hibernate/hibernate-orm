/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.id.enhanced;

import java.io.Serializable;
import java.util.Properties;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.ObjectNameNormalizer;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.BulkInsertionCapableIdentifierGenerator;
import org.hibernate.id.Configurable;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.mapping.Table;
import org.hibernate.type.Type;

/**
 * Generates identifier values based on an sequence-style database structure.
 * Variations range from actually using a sequence to using a table to mimic
 * a sequence.  These variations are encapsulated by the {@link DatabaseStructure}
 * interface internally.
 * <p/>
 * <b>NOTE</b> that by default we utilize a single database sequence for all
 * generators.  The configuration parameter {@link #CONFIG_PREFER_SEQUENCE_PER_ENTITY}
 * can be used to create dedicated sequence for each entity based on its name.
 * Sequence suffix can be controlled with {@link #CONFIG_SEQUENCE_PER_ENTITY_SUFFIX}
 * option.
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
 *     <td>{@link #FORCE_TBL_PARAM}</td>
 *     <td><b><i>false<i/></b></td>
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
		implements PersistentIdentifierGenerator, BulkInsertionCapableIdentifierGenerator, Configurable {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			SequenceStyleGenerator.class.getName()
	);

	// general purpose parameters ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	public static final String SEQUENCE_PARAM = "sequence_name";
	public static final String DEF_SEQUENCE_NAME = "hibernate_sequence";

	public static final String INITIAL_PARAM = "initial_value";
	public static final int DEFAULT_INITIAL_VALUE = 1;

	public static final String INCREMENT_PARAM = "increment_size";
	public static final int DEFAULT_INCREMENT_SIZE = 1;

	public static final String OPT_PARAM = "optimizer";

	public static final String FORCE_TBL_PARAM = "force_table_use";

	public static final String CONFIG_PREFER_SEQUENCE_PER_ENTITY = "prefer_sequence_per_entity";
	public static final String CONFIG_SEQUENCE_PER_ENTITY_SUFFIX = "sequence_per_entity_suffix";
	public static final String DEF_SEQUENCE_SUFFIX = "_SEQ";


	// table-specific parameters ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	public static final String VALUE_COLUMN_PARAM = "value_column";
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
	public void configure(Type type, Properties params, Dialect dialect) throws MappingException {
		this.identifierType = type;
		boolean forceTableUse = ConfigurationHelper.getBoolean( FORCE_TBL_PARAM, params, false );

		final String sequenceName = determineSequenceName( params, dialect );

		final int initialValue = determineInitialValue( params );
		int incrementSize = determineIncrementSize( params );

		final String optimizationStrategy = determineOptimizationStrategy( params, incrementSize );
		incrementSize = determineAdjustedIncrementSize( optimizationStrategy, incrementSize );

		if ( dialect.supportsSequences() && !forceTableUse ) {
			if ( !dialect.supportsPooledSequences() && OptimizerFactory.isPooledOptimizer( optimizationStrategy ) ) {
				forceTableUse = true;
                LOG.forcingTableUse();
			}
		}

		this.databaseStructure = buildDatabaseStructure(
				type,
				params,
				dialect,
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
		this.databaseStructure.prepare( optimizer );
	}

	/**
	 * Determine the name of the sequence (or table if this resolves to a physical table)
	 * to use.
	 * <p/>
	 * Called during {@link #configure configuration}.
	 *
	 * @param params The params supplied in the generator config (plus some standard useful extras).
	 * @param dialect The dialect in effect
	 * @return The sequence name
	 */
	protected String determineSequenceName(Properties params, Dialect dialect) {
		String sequencePerEntitySuffix = ConfigurationHelper.getString( CONFIG_SEQUENCE_PER_ENTITY_SUFFIX, params, DEF_SEQUENCE_SUFFIX );
		// JPA_ENTITY_NAME value honors <class ... entity-name="..."> (HBM) and @Entity#name (JPA) overrides.
		String sequenceName = ConfigurationHelper.getBoolean( CONFIG_PREFER_SEQUENCE_PER_ENTITY, params, false )
				? params.getProperty( JPA_ENTITY_NAME ) + sequencePerEntitySuffix
				: DEF_SEQUENCE_NAME;
		ObjectNameNormalizer normalizer = ( ObjectNameNormalizer ) params.get( IDENTIFIER_NORMALIZER );
		sequenceName = ConfigurationHelper.getString( SEQUENCE_PARAM, params, sequenceName );
		if ( sequenceName.indexOf( '.' ) < 0 ) {
			sequenceName = normalizer.normalizeIdentifierQuoting( sequenceName );
			String schemaName = params.getProperty( SCHEMA );
			String catalogName = params.getProperty( CATALOG );
			sequenceName = Table.qualify(
					dialect.quote( catalogName ),
					dialect.quote( schemaName ),
					dialect.quote( sequenceName )
			);
		}
		else {
			// if already qualified there is not much we can do in a portable manner so we pass it
			// through and assume the user has set up the name correctly.
		}
		return sequenceName;
	}

	/**
	 * Determine the name of the column used to store the generator value in
	 * the db.
	 * <p/>
	 * Called during {@link #configure configuration} <b>when resolving to a
	 * physical table</b>.
	 *
	 * @param params The params supplied in the generator config (plus some standard useful extras).
	 * @param dialect The dialect in effect.
	 * @return The value column name
	 */
	protected String determineValueColumnName(Properties params, Dialect dialect) {
		ObjectNameNormalizer normalizer = ( ObjectNameNormalizer ) params.get( IDENTIFIER_NORMALIZER );
		String name = ConfigurationHelper.getString( VALUE_COLUMN_PARAM, params, DEF_VALUE_COLUMN );
		return dialect.quote( normalizer.normalizeIdentifierQuoting( name ) );
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
	protected String determineOptimizationStrategy(Properties params, int incrementSize) {
		// if the increment size is greater than one, we prefer pooled optimization; but we first
		// need to see if the user prefers POOL or POOL_LO...
		String defaultPooledOptimizerStrategy = ConfigurationHelper.getBoolean( Environment.PREFER_POOLED_VALUES_LO, params, false )
				? OptimizerFactory.StandardOptimizerDescriptor.POOLED_LO.getExternalName()
				: OptimizerFactory.StandardOptimizerDescriptor.POOLED.getExternalName();
		String defaultOptimizerStrategy = incrementSize <= 1
				? OptimizerFactory.StandardOptimizerDescriptor.NONE.getExternalName()
				: defaultPooledOptimizerStrategy;
		return ConfigurationHelper.getString( OPT_PARAM, params, defaultOptimizerStrategy );
	}

	/**
	 * In certain cases we need to adjust the increment size based on the
	 * selected optimizer.  This is the hook to achieve that.
	 *
	 * @param optimizationStrategy The optimizer strategy (name)
	 * @param incrementSize The {@link #determineIncrementSize determined increment size}
	 * @return The adjusted increment size.
	 */
	protected int determineAdjustedIncrementSize(String optimizationStrategy, int incrementSize) {
		if ( incrementSize > 1
				&& OptimizerFactory.StandardOptimizerDescriptor.NONE.getExternalName().equals( optimizationStrategy ) ) {
            LOG.honoringOptimizerSetting(
					OptimizerFactory.StandardOptimizerDescriptor.NONE.getExternalName(),
					INCREMENT_PARAM,
					incrementSize
			);
			incrementSize = 1;
		}
		return incrementSize;
	}

	/**
	 * Build the database structure.
	 *
	 * @param type The Hibernate type of the identifier property
	 * @param params The params supplied in the generator config (plus some standard useful extras).
	 * @param dialect The dialect being used.
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
			Dialect dialect,
			boolean forceTableUse,
			String sequenceName,
			int initialValue,
			int incrementSize) {
		boolean useSequence = dialect.supportsSequences() && !forceTableUse;
		if ( useSequence ) {
			return new SequenceStructure( dialect, sequenceName, initialValue, incrementSize, type.getReturnedClass() );
		}
		else {
			String valueColumnName = determineValueColumnName( params, dialect );
			return new TableStructure( dialect, sequenceName, valueColumnName, initialValue, incrementSize, type.getReturnedClass() );
		}
	}


	// IdentifierGenerator implementation ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public Serializable generate(SessionImplementor session, Object object) throws HibernateException {
		return optimizer.generate( databaseStructure.buildCallback( session ) );
	}


	// PersistentIdentifierGenerator implementation ~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public Object generatorKey() {
		return databaseStructure.getName();
	}

	@Override
	public String[] sqlCreateStrings(Dialect dialect) throws HibernateException {
		return databaseStructure.sqlCreateStrings( dialect );
	}

	@Override
	public String[] sqlDropStrings(Dialect dialect) throws HibernateException {
		return databaseStructure.sqlDropStrings( dialect );
	}


	// BulkInsertionCapableIdentifierGenerator implementation ~~~~~~~~~~~~~~~~~

	@Override
	public boolean supportsBulkInsertionIdentifierGeneration() {
		// it does, as long as
		// 		1) there is no (non-noop) optimizer in use
		//		2) the underlying structure is a sequence
		return OptimizerFactory.NoopOptimizer.class.isInstance( getOptimizer() )
				&& getDatabaseStructure().isPhysicalSequence();
	}

	@Override
	public String determineBulkInsertionIdentifierGenerationSelectFragment(Dialect dialect) {
		return dialect.getSelectSequenceNextValString( getDatabaseStructure().getName() );
	}
}
