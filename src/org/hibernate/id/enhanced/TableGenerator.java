package org.hibernate.id.enhanced;

import java.sql.Types;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;
import java.util.HashMap;
import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hibernate.engine.TransactionHelper;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.Configurable;
import org.hibernate.type.Type;
import org.hibernate.dialect.Dialect;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.LockMode;
import org.hibernate.mapping.Table;
import org.hibernate.util.PropertiesHelper;
import org.hibernate.util.StringHelper;
import org.hibernate.util.CollectionHelper;

/**
 * A "segmented" version of the enhanced table generator.  The term "segmented"
 * refers to the fact that this table can hold multiple value generators,
 * segmented by a key.
 * <p/>
 * Configuration parameters:
 * <table>
 * 	 <tr>
 *     <td><b>NAME</b></td>
 *     <td><b>DEFAULT</b></td>
 *     <td><b>DESCRIPTION</b></td>
 *   </tr>
 *   <tr>
 *     <td>{@link #TABLE_PARAM}</td>
 *     <td>{@link #DEF_TABLE}</td>
 *     <td>The name of the table to use to store/retrieve values</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #VALUE_COLUMN_PARAM}</td>
 *     <td>{@link #DEF_VALUE_COLUMN}</td>
 *     <td>The name of column which holds the sequence value for the given segment</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #SEGMENT_COLUMN_PARAM}</td>
 *     <td>{@link #DEF_SEGMENT_COLUMN}</td>
 *     <td>The name of the column which holds the segment key</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #SEGMENT_VALUE_PARAM}</td>
 *     <td>{@link #DEF_SEGMENT_VALUE}</td>
 *     <td>The value indicating which segment is used by this generator; refers to values in the {@link #SEGMENT_COLUMN_PARAM} column</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #SEGMENT_LENGTH_PARAM}</td>
 *     <td>{@link #DEF_SEGMENT_LENGTH}</td>
 *     <td>The data length of the {@link #SEGMENT_COLUMN_PARAM} column; used for schema creation</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #INITIAL_PARAM}</td>
 *     <td>{@link #DEFAULT_INITIAL_VALUE}</td>
 *     <td>The initial value to be stored for the given segment</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #INCREMENT_PARAM}</td>
 *     <td>{@link #DEFAULT_INCREMENT_SIZE}</td>
 *     <td>The increment size for the underlying segment; see the discussion on {@link Optimizer} for more details.</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #OPT_PARAM}</td>
 *     <td><i>depends on defined increment size</i></td>
 *     <td>Allows explicit definition of which optimization strategy to use</td>
 *   </tr>
 * </table>
 *
 * @author Steve Ebersole
 */
public class TableGenerator extends TransactionHelper implements PersistentIdentifierGenerator, Configurable {
	private static final Log log = LogFactory.getLog( TableGenerator.class );

	public static final String TABLE_PARAM = "table_name";
	public static final String DEF_TABLE = "hibernate_sequences";

	public static final String VALUE_COLUMN_PARAM = "value_column_name";
	public static final String DEF_VALUE_COLUMN = "next_val";

	public static final String SEGMENT_COLUMN_PARAM = "segment_column_name";
	public static final String DEF_SEGMENT_COLUMN = "sequence_name";

	public static final String SEGMENT_VALUE_PARAM = "segment_value";
	public static final String DEF_SEGMENT_VALUE = "default";

	public static final String SEGMENT_LENGTH_PARAM = "segment_value_length";
	public static final int DEF_SEGMENT_LENGTH = 255;

	public static final String INITIAL_PARAM = "initial_value";
	public static final int DEFAULT_INITIAL_VALUE = 1;

	public static final String INCREMENT_PARAM = "increment_size";
	public static final int DEFAULT_INCREMENT_SIZE = 1;

	public static final String OPT_PARAM = "optimizer";


	private String tableName;
	private String valueColumnName;
	private String segmentColumnName;
	private String segmentValue;
	private int segmentValueLength;
	private int initialValue;
	private int incrementSize;

	private Type identifierType;

	private String query;
	private String insert;
	private String update;

	private Optimizer optimizer;
	private long accessCount = 0;

	public String getTableName() {
		return tableName;
	}

	public String getSegmentColumnName() {
		return segmentColumnName;
	}

	public String getSegmentValue() {
		return segmentValue;
	}

	public int getSegmentValueLength() {
		return segmentValueLength;
	}

	public String getValueColumnName() {
		return valueColumnName;
	}

	public Type getIdentifierType() {
		return identifierType;
	}

	public int getInitialValue() {
		return initialValue;
	}

	public int getIncrementSize() {
		return incrementSize;
	}

	public Optimizer getOptimizer() {
		return optimizer;
	}

	public long getTableAccessCount() {
		return accessCount;
	}

	public void configure(Type type, Properties params, Dialect dialect) throws MappingException {
		tableName = PropertiesHelper.getString( TABLE_PARAM, params, DEF_TABLE );
		if ( tableName.indexOf( '.' ) < 0 ) {
			String schemaName = params.getProperty( SCHEMA );
			String catalogName = params.getProperty( CATALOG );
			tableName = Table.qualify( catalogName, schemaName, tableName );
		}

		segmentColumnName = PropertiesHelper.getString( SEGMENT_COLUMN_PARAM, params, DEF_SEGMENT_COLUMN );
		segmentValue = params.getProperty( SEGMENT_VALUE_PARAM );
		if ( StringHelper.isEmpty( segmentValue ) ) {
			log.debug( "explicit segment value for id generator [" + tableName + '.' + segmentColumnName + "] suggested; using default [" + DEF_SEGMENT_VALUE + "]" );
			segmentValue = DEF_SEGMENT_VALUE;
		}
		segmentValueLength = PropertiesHelper.getInt( SEGMENT_LENGTH_PARAM, params, DEF_SEGMENT_LENGTH );
		valueColumnName = PropertiesHelper.getString( VALUE_COLUMN_PARAM, params, DEF_VALUE_COLUMN );
		initialValue = PropertiesHelper.getInt( INITIAL_PARAM, params, DEFAULT_INITIAL_VALUE );
		incrementSize = PropertiesHelper.getInt( INCREMENT_PARAM, params, DEFAULT_INCREMENT_SIZE );
		identifierType = type;

		String query = "select " + valueColumnName +
				" from " + tableName + " tbl" +
				" where tbl." + segmentColumnName + "=?";
		HashMap lockMap = new HashMap();
		lockMap.put( "tbl", LockMode.UPGRADE );
		this.query = dialect.applyLocksToSql( query, lockMap, CollectionHelper.EMPTY_MAP );

		update = "update " + tableName +
				" set " + valueColumnName + "=? " +
				" where " + valueColumnName + "=? and " + segmentColumnName + "=?";

		insert = "insert into " + tableName + " (" + segmentColumnName + ", " + valueColumnName + ") " + " values (?,?)";

		String defOptStrategy = incrementSize <= 1 ? OptimizerFactory.NONE : OptimizerFactory.POOL;
		String optimizationStrategy = PropertiesHelper.getString( OPT_PARAM, params, defOptStrategy );
		optimizer = OptimizerFactory.buildOptimizer( optimizationStrategy, identifierType.getReturnedClass(), incrementSize );
	}

	public synchronized Serializable generate(final SessionImplementor session, Object obj) {
		return optimizer.generate(
				new AccessCallback() {
					public long getNextValue() {
						return ( ( Number ) doWorkInNewTransaction( session ) ).longValue();
					}
				}
		);
	}

	public Serializable doWorkInCurrentTransaction(Connection conn, String sql) throws SQLException {
		int result;
		int rows;
		do {
			sql = query;
			SQL.debug( sql );
			PreparedStatement queryPS = conn.prepareStatement( query );
			try {
				queryPS.setString( 1, segmentValue );
				ResultSet queryRS = queryPS.executeQuery();
				if ( !queryRS.next() ) {
					PreparedStatement insertPS = null;
					try {
						result = initialValue;
						sql = insert;
						SQL.debug( sql );
						insertPS = conn.prepareStatement( insert );
						insertPS.setString( 1, segmentValue );
						insertPS.setLong( 2, result );
						insertPS.execute();
					}
					finally {
						if ( insertPS != null ) {
							insertPS.close();
						}
					}
				}
				else {
					result = queryRS.getInt( 1 );
				}
				queryRS.close();
			}
			catch ( SQLException sqle ) {
				log.error( "could not read or init a hi value", sqle );
				throw sqle;
			}
			finally {
				queryPS.close();
			}

			sql = update;
			SQL.debug( sql );
			PreparedStatement updatePS = conn.prepareStatement( update );
			try {
				long newValue = optimizer.applyIncrementSizeToSourceValues()
						? result + incrementSize : result + 1;
				updatePS.setLong( 1, newValue );
				updatePS.setLong( 2, result );
				updatePS.setString( 3, segmentValue );
				rows = updatePS.executeUpdate();
			}
			catch ( SQLException sqle ) {
				log.error( "could not update hi value in: " + tableName, sqle );
				throw sqle;
			}
			finally {
				updatePS.close();
			}
		}
		while ( rows == 0 );

		accessCount++;

		return new Integer( result );
	}

	public String[] sqlCreateStrings(Dialect dialect) throws HibernateException {
		return new String[] {
				new StringBuffer()
						.append( dialect.getCreateTableString() )
						.append( ' ' )
						.append( tableName )
						.append( " ( " )
						.append( segmentColumnName )
						.append( ' ' )
						.append( dialect.getTypeName( Types.VARCHAR, segmentValueLength, 0, 0 ) )
						.append( ",  " )
						.append( valueColumnName )
						.append( ' ' )
						.append( dialect.getTypeName( Types.BIGINT ) )
						.append( " ) " )
						.toString()
		};
	}

	public String[] sqlDropStrings(Dialect dialect) throws HibernateException {
		StringBuffer sqlDropString = new StringBuffer().append( "drop table " );
		if ( dialect.supportsIfExistsBeforeTableName() ) {
			sqlDropString.append( "if exists " );
		}
		sqlDropString.append( tableName ).append( dialect.getCascadeConstraintsString() );
		if ( dialect.supportsIfExistsAfterTableName() ) {
			sqlDropString.append( " if exists" );
		}
		return new String[] { sqlDropString.toString() };
	}

	public Object generatorKey() {
		return tableName;
	}
}
