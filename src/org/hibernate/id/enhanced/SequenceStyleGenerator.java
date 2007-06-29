package org.hibernate.id.enhanced;

import java.util.Properties;
import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.Configurable;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.mapping.Table;
import org.hibernate.util.PropertiesHelper;
import org.hibernate.type.Type;
import org.hibernate.dialect.Dialect;

/**
 * Generates identifier values based on an sequence-style database structure.
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
 */
public class SequenceStyleGenerator implements PersistentIdentifierGenerator, Configurable {
	private static final Log log = LogFactory.getLog( SequenceStyleGenerator.class );

	// general purpose parameters ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	public static final String SEQUENCE_PARAM = "sequence_name";
	public static final String DEF_SEQUENCE_NAME = "hibernate_sequence";

	public static final String INITIAL_PARAM = "initial_value";
	public static final int DEFAULT_INITIAL_VALUE = 1;

	public static final String INCREMENT_PARAM = "increment_size";
	public static final int DEFAULT_INCREMENT_SIZE = 1;

	public static final String OPT_PARAM = "optimizer";

	public static final String FORCE_TBL_PARAM = "force_table_use";


	// table-specific parameters ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	public static final String VALUE_COLUMN_PARAM = "value_column";
	public static final String DEF_VALUE_COLUMN = "next_val";


	// state ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private DatabaseStructure databaseStructure;
	private Optimizer optimizer;
	private Type identifierType;

	public DatabaseStructure getDatabaseStructure() {
		return databaseStructure;
	}

	public Optimizer getOptimizer() {
		return optimizer;
	}

	public Type getIdentifierType() {
		return identifierType;
	}


	// Configurable implementation ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public void configure(Type type, Properties params, Dialect dialect) throws MappingException {
		identifierType = type;
		boolean forceTableUse = PropertiesHelper.getBoolean( FORCE_TBL_PARAM, params, false );

		String sequenceName = PropertiesHelper.getString( SEQUENCE_PARAM, params, DEF_SEQUENCE_NAME );
		if ( sequenceName.indexOf( '.' ) < 0 ) {
			String schemaName = params.getProperty( SCHEMA );
			String catalogName = params.getProperty( CATALOG );
			sequenceName = Table.qualify( catalogName, schemaName, sequenceName );
		}
		int initialValue = PropertiesHelper.getInt( INITIAL_PARAM, params, DEFAULT_INITIAL_VALUE );
		int incrementSize = PropertiesHelper.getInt( INCREMENT_PARAM, params, DEFAULT_INCREMENT_SIZE );

		String valueColumnName = PropertiesHelper.getString( VALUE_COLUMN_PARAM, params, DEF_VALUE_COLUMN );

		String defOptStrategy = incrementSize <= 1 ? OptimizerFactory.NONE : OptimizerFactory.POOL;
		String optimizationStrategy = PropertiesHelper.getString( OPT_PARAM, params, defOptStrategy );
		if ( OptimizerFactory.NONE.equals( optimizationStrategy ) && incrementSize > 1 ) {
			log.warn( "config specified explicit optimizer of [" + OptimizerFactory.NONE + "], but [" + INCREMENT_PARAM + "=" + incrementSize + "; honoring optimizer setting" );
			incrementSize = 1;
		}
		if ( dialect.supportsSequences() && !forceTableUse ) {
			if ( OptimizerFactory.POOL.equals( optimizationStrategy ) && !dialect.supportsPooledSequences() ) {
				// TODO : may even be better to fall back to a pooled table strategy here so that the db stored values remain consistent...
				optimizationStrategy = OptimizerFactory.HILO;
			}
			databaseStructure = new SequenceStructure( dialect, sequenceName, initialValue, incrementSize );
		}
		else {
			databaseStructure = new TableStructure( dialect, sequenceName, valueColumnName, initialValue, incrementSize );
		}

		optimizer = OptimizerFactory.buildOptimizer( optimizationStrategy, identifierType.getReturnedClass(), incrementSize );
		databaseStructure.prepare( optimizer );
	}


	// IdentifierGenerator implementation ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public Serializable generate(SessionImplementor session, Object object) throws HibernateException {
		return optimizer.generate( databaseStructure.buildCallback( session ) );
	}


	// PersistentIdentifierGenerator implementation ~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public Object generatorKey() {
		return databaseStructure.getName();
	}

	public String[] sqlCreateStrings(Dialect dialect) throws HibernateException {
		return databaseStructure.sqlCreateStrings( dialect );
	}

	public String[] sqlDropStrings(Dialect dialect) throws HibernateException {
		return databaseStructure.sqlDropStrings( dialect );
	}

}
