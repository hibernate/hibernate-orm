/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.NullPrecedence;
import org.hibernate.ScrollMode;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.CastFunction;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardAnsiSqlAggregationFunctions;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.OptimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.OptimisticLockingStrategy;
import org.hibernate.dialect.lock.PessimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.PessimisticReadSelectLockingStrategy;
import org.hibernate.dialect.lock.PessimisticWriteSelectLockingStrategy;
import org.hibernate.dialect.lock.SelectLockingStrategy;
import org.hibernate.dialect.pagination.LegacyLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.unique.DefaultUniqueDelegate;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.env.internal.DefaultSchemaNameResolver;
import org.hibernate.engine.jdbc.env.spi.AnsiSqlKeywords;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.engine.jdbc.env.spi.SchemaNameResolver;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.exception.spi.ConversionContext;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.SQLExceptionConverter;
import org.hibernate.exception.spi.ViolatedConstraintNameExtracter;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.hql.spi.id.persistent.PersistentTableBulkIdStrategy;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.id.SequenceGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.internal.util.io.StreamCopier;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Constraint;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.Table;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.procedure.internal.StandardCallableStatementSupport;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ANSICaseFragment;
import org.hibernate.sql.ANSIJoinFragment;
import org.hibernate.sql.CaseFragment;
import org.hibernate.sql.ForUpdateFragment;
import org.hibernate.sql.JoinFragment;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorLegacyImpl;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorNoOpImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.internal.StandardAuxiliaryDatabaseObjectExporter;
import org.hibernate.tool.schema.internal.StandardForeignKeyExporter;
import org.hibernate.tool.schema.internal.StandardIndexExporter;
import org.hibernate.tool.schema.internal.StandardSequenceExporter;
import org.hibernate.tool.schema.internal.StandardTableExporter;
import org.hibernate.tool.schema.internal.StandardUniqueKeyExporter;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.sql.ClobTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

import org.jboss.logging.Logger;

/**
 * Represents a dialect of SQL implemented by a particular RDBMS.  Subclasses implement Hibernate compatibility
 * with different systems.  Subclasses should provide a public default constructor that register a set of type
 * mappings and default Hibernate properties.  Subclasses should be immutable.
 *
 * @author Gavin King, David Channon
 */
@SuppressWarnings("deprecation")
public abstract class Dialect implements ConversionContext {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			Dialect.class.getName()
	);

	/**
	 * Defines a default batch size constant
	 */
	public static final String DEFAULT_BATCH_SIZE = "15";

	/**
	 * Defines a "no batching" batch size constant
	 */
	public static final String NO_BATCH = "0";

	/**
	 * Characters used as opening for quoting SQL identifiers
	 */
	public static final String QUOTE = "`\"[";

	/**
	 * Characters used as closing for quoting SQL identifiers
	 */
	public static final String CLOSED_QUOTE = "`\"]";

	private final TypeNames typeNames = new TypeNames();
	private final TypeNames hibernateTypeNames = new TypeNames();

	private final Properties properties = new Properties();
	private final Map<String, SQLFunction> sqlFunctions = new HashMap<String, SQLFunction>();
	private final Set<String> sqlKeywords = new HashSet<String>();

	private final UniqueDelegate uniqueDelegate;


	// constructors and factory methods ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	protected Dialect() {
		LOG.usingDialect( this );
		StandardAnsiSqlAggregationFunctions.primeFunctionMap( sqlFunctions );

		// standard sql92 functions (can be overridden by subclasses)
		registerFunction( "substring", new SQLFunctionTemplate( StandardBasicTypes.STRING, "substring(?1, ?2, ?3)" ) );
		registerFunction( "locate", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "locate(?1, ?2, ?3)" ) );
		registerFunction( "trim", new SQLFunctionTemplate( StandardBasicTypes.STRING, "trim(?1 ?2 ?3 ?4)" ) );
		registerFunction( "length", new StandardSQLFunction( "length", StandardBasicTypes.INTEGER ) );
		registerFunction( "bit_length", new StandardSQLFunction( "bit_length", StandardBasicTypes.INTEGER ) );
		registerFunction( "coalesce", new StandardSQLFunction( "coalesce" ) );
		registerFunction( "nullif", new StandardSQLFunction( "nullif" ) );
		registerFunction( "abs", new StandardSQLFunction( "abs" ) );
		registerFunction( "mod", new StandardSQLFunction( "mod", StandardBasicTypes.INTEGER) );
		registerFunction( "sqrt", new StandardSQLFunction( "sqrt", StandardBasicTypes.DOUBLE) );
		registerFunction( "upper", new StandardSQLFunction("upper") );
		registerFunction( "lower", new StandardSQLFunction("lower") );
		registerFunction( "cast", new CastFunction() );
		registerFunction( "extract", new SQLFunctionTemplate(StandardBasicTypes.INTEGER, "extract(?1 ?2 ?3)") );

		//map second/minute/hour/day/month/year to ANSI extract(), override on subclasses
		registerFunction( "second", new SQLFunctionTemplate(StandardBasicTypes.INTEGER, "extract(second from ?1)") );
		registerFunction( "minute", new SQLFunctionTemplate(StandardBasicTypes.INTEGER, "extract(minute from ?1)") );
		registerFunction( "hour", new SQLFunctionTemplate(StandardBasicTypes.INTEGER, "extract(hour from ?1)") );
		registerFunction( "day", new SQLFunctionTemplate(StandardBasicTypes.INTEGER, "extract(day from ?1)") );
		registerFunction( "month", new SQLFunctionTemplate(StandardBasicTypes.INTEGER, "extract(month from ?1)") );
		registerFunction( "year", new SQLFunctionTemplate(StandardBasicTypes.INTEGER, "extract(year from ?1)") );

		registerFunction( "str", new SQLFunctionTemplate(StandardBasicTypes.STRING, "cast(?1 as char)") );

		registerColumnType( Types.BIT, "bit" );
		registerColumnType( Types.BOOLEAN, "boolean" );
		registerColumnType( Types.TINYINT, "tinyint" );
		registerColumnType( Types.SMALLINT, "smallint" );
		registerColumnType( Types.INTEGER, "integer" );
		registerColumnType( Types.BIGINT, "bigint" );
		registerColumnType( Types.FLOAT, "float($p)" );
		registerColumnType( Types.DOUBLE, "double precision" );
		registerColumnType( Types.NUMERIC, "numeric($p,$s)" );
		registerColumnType( Types.REAL, "real" );

		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.TIME, "time" );
		registerColumnType( Types.TIMESTAMP, "timestamp" );

		registerColumnType( Types.VARBINARY, "bit varying($l)" );
		registerColumnType( Types.LONGVARBINARY, "bit varying($l)" );
		registerColumnType( Types.BLOB, "blob" );

		registerColumnType( Types.CHAR, "char($l)" );
		registerColumnType( Types.VARCHAR, "varchar($l)" );
		registerColumnType( Types.LONGVARCHAR, "varchar($l)" );
		registerColumnType( Types.CLOB, "clob" );

		registerColumnType( Types.NCHAR, "nchar($l)" );
		registerColumnType( Types.NVARCHAR, "nvarchar($l)" );
		registerColumnType( Types.LONGNVARCHAR, "nvarchar($l)" );
		registerColumnType( Types.NCLOB, "nclob" );

		// register hibernate types for default use in scalar sqlquery type auto detection
		registerHibernateType( Types.BIGINT, StandardBasicTypes.BIG_INTEGER.getName() );
		registerHibernateType( Types.BINARY, StandardBasicTypes.BINARY.getName() );
		registerHibernateType( Types.BIT, StandardBasicTypes.BOOLEAN.getName() );
		registerHibernateType( Types.BOOLEAN, StandardBasicTypes.BOOLEAN.getName() );
		registerHibernateType( Types.CHAR, StandardBasicTypes.CHARACTER.getName() );
		registerHibernateType( Types.CHAR, 1, StandardBasicTypes.CHARACTER.getName() );
		registerHibernateType( Types.CHAR, 255, StandardBasicTypes.STRING.getName() );
		registerHibernateType( Types.DATE, StandardBasicTypes.DATE.getName() );
		registerHibernateType( Types.DOUBLE, StandardBasicTypes.DOUBLE.getName() );
		registerHibernateType( Types.FLOAT, StandardBasicTypes.FLOAT.getName() );
		registerHibernateType( Types.INTEGER, StandardBasicTypes.INTEGER.getName() );
		registerHibernateType( Types.SMALLINT, StandardBasicTypes.SHORT.getName() );
		registerHibernateType( Types.TINYINT, StandardBasicTypes.BYTE.getName() );
		registerHibernateType( Types.TIME, StandardBasicTypes.TIME.getName() );
		registerHibernateType( Types.TIMESTAMP, StandardBasicTypes.TIMESTAMP.getName() );
		registerHibernateType( Types.VARCHAR, StandardBasicTypes.STRING.getName() );
		registerHibernateType( Types.VARBINARY, StandardBasicTypes.BINARY.getName() );
		registerHibernateType( Types.LONGVARCHAR, StandardBasicTypes.TEXT.getName() );
		registerHibernateType( Types.LONGVARBINARY, StandardBasicTypes.IMAGE.getName() );
		registerHibernateType( Types.NUMERIC, StandardBasicTypes.BIG_DECIMAL.getName() );
		registerHibernateType( Types.DECIMAL, StandardBasicTypes.BIG_DECIMAL.getName() );
		registerHibernateType( Types.BLOB, StandardBasicTypes.BLOB.getName() );
		registerHibernateType( Types.CLOB, StandardBasicTypes.CLOB.getName() );
		registerHibernateType( Types.REAL, StandardBasicTypes.FLOAT.getName() );

		uniqueDelegate = new DefaultUniqueDelegate( this );
	}

	/**
	 * Get an instance of the dialect specified by the current <tt>System</tt> properties.
	 *
	 * @return The specified Dialect
	 * @throws HibernateException If no dialect was specified, or if it could not be instantiated.
	 */
	public static Dialect getDialect() throws HibernateException {
		return instantiateDialect( Environment.getProperties().getProperty( Environment.DIALECT ) );
	}


	/**
	 * Get an instance of the dialect specified by the given properties or by
	 * the current <tt>System</tt> properties.
	 *
	 * @param props The properties to use for finding the dialect class to use.
	 * @return The specified Dialect
	 * @throws HibernateException If no dialect was specified, or if it could not be instantiated.
	 */
	public static Dialect getDialect(Properties props) throws HibernateException {
		final String dialectName = props.getProperty( Environment.DIALECT );
		if ( dialectName == null ) {
			return getDialect();
		}
		return instantiateDialect( dialectName );
	}

	private static Dialect instantiateDialect(String dialectName) throws HibernateException {
		if ( dialectName == null ) {
			throw new HibernateException( "The dialect was not set. Set the property hibernate.dialect." );
		}
		try {
			return (Dialect) ReflectHelper.classForName( dialectName ).newInstance();
		}
		catch ( ClassNotFoundException cnfe ) {
			throw new HibernateException( "Dialect class not found: " + dialectName );
		}
		catch ( Exception e ) {
			throw new HibernateException( "Could not instantiate given dialect class: " + dialectName, e );
		}
	}

	/**
	 * Retrieve a set of default Hibernate properties for this database.
	 *
	 * @return a set of Hibernate properties
	 */
	public final Properties getDefaultProperties() {
		return properties;
	}

	@Override
	public String toString() {
		return getClass().getName();
	}


	// database type mapping support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Allows the Dialect to contribute additional types
	 *
	 * @param typeContributions Callback to contribute the types
	 * @param serviceRegistry The service registry
	 */
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		// by default, nothing to do
	}

	/**
	 * Get the name of the database type associated with the given
	 * {@link java.sql.Types} typecode.
	 *
	 * @param code The {@link java.sql.Types} typecode
	 * @return the database type name
	 * @throws HibernateException If no mapping was specified for that type.
	 */
	public String getTypeName(int code) throws HibernateException {
		final String result = typeNames.get( code );
		if ( result == null ) {
			throw new HibernateException( "No default type mapping for (java.sql.Types) " + code );
		}
		return result;
	}

	/**
	 * Get the name of the database type associated with the given
	 * {@link java.sql.Types} typecode with the given storage specification
	 * parameters.
	 *
	 * @param code The {@link java.sql.Types} typecode
	 * @param length The datatype length
	 * @param precision The datatype precision
	 * @param scale The datatype scale
	 * @return the database type name
	 * @throws HibernateException If no mapping was specified for that type.
	 */
	public String getTypeName(int code, long length, int precision, int scale) throws HibernateException {
		final String result = typeNames.get( code, length, precision, scale );
		if ( result == null ) {
			throw new HibernateException(
					String.format( "No type mapping for java.sql.Types code: %s, length: %s", code, length )
			);
		}
		return result;
	}

	/**
	 * Get the name of the database type appropriate for casting operations
	 * (via the CAST() SQL function) for the given {@link java.sql.Types} typecode.
	 *
	 * @param code The {@link java.sql.Types} typecode
	 * @return The database type name
	 */
	public String getCastTypeName(int code) {
		return getTypeName( code, Column.DEFAULT_LENGTH, Column.DEFAULT_PRECISION, Column.DEFAULT_SCALE );
	}

	/**
	 * Return an expression casting the value to the specified type
	 *
	 * @param value The value to cast
	 * @param jdbcTypeCode The JDBC type code to cast to
	 * @param length The type length
	 * @param precision The type precision
	 * @param scale The type scale
	 *
	 * @return The cast expression
	 */
	public String cast(String value, int jdbcTypeCode, int length, int precision, int scale) {
		if ( jdbcTypeCode == Types.CHAR ) {
			return "cast(" + value + " as char(" + length + "))";
		}
		else {
			return "cast(" + value + "as " + getTypeName( jdbcTypeCode, length, precision, scale ) + ")";
		}
	}

	/**
	 * Return an expression casting the value to the specified type.  Simply calls
	 * {@link #cast(String, int, int, int, int)} passing {@link Column#DEFAULT_PRECISION} and
	 * {@link Column#DEFAULT_SCALE} as the precision/scale.
	 *
	 * @param value The value to cast
	 * @param jdbcTypeCode The JDBC type code to cast to
	 * @param length The type length
	 *
	 * @return The cast expression
	 */
	public String cast(String value, int jdbcTypeCode, int length) {
		return cast( value, jdbcTypeCode, length, Column.DEFAULT_PRECISION, Column.DEFAULT_SCALE );
	}

	/**
	 * Return an expression casting the value to the specified type.  Simply calls
	 * {@link #cast(String, int, int, int, int)} passing {@link Column#DEFAULT_LENGTH} as the length
	 *
	 * @param value The value to cast
	 * @param jdbcTypeCode The JDBC type code to cast to
	 * @param precision The type precision
	 * @param scale The type scale
	 *
	 * @return The cast expression
	 */
	public String cast(String value, int jdbcTypeCode, int precision, int scale) {
		return cast( value, jdbcTypeCode, Column.DEFAULT_LENGTH, precision, scale );
	}

	/**
	 * Subclasses register a type name for the given type code and maximum
	 * column length. <tt>$l</tt> in the type name with be replaced by the
	 * column length (if appropriate).
	 *
	 * @param code The {@link java.sql.Types} typecode
	 * @param capacity The maximum length of database type
	 * @param name The database type name
	 */
	protected void registerColumnType(int code, long capacity, String name) {
		typeNames.put( code, capacity, name );
	}

	/**
	 * Subclasses register a type name for the given type code. <tt>$l</tt> in
	 * the type name with be replaced by the column length (if appropriate).
	 *
	 * @param code The {@link java.sql.Types} typecode
	 * @param name The database type name
	 */
	protected void registerColumnType(int code, String name) {
		typeNames.put( code, name );
	}

	/**
	 * Allows the dialect to override a {@link SqlTypeDescriptor}.
	 * <p/>
	 * If the passed {@code sqlTypeDescriptor} allows itself to be remapped (per
	 * {@link org.hibernate.type.descriptor.sql.SqlTypeDescriptor#canBeRemapped()}), then this method uses
	 * {@link #getSqlTypeDescriptorOverride}  to get an optional override based on the SQL code returned by
	 * {@link SqlTypeDescriptor#getSqlType()}.
	 * <p/>
	 * If this dialect does not provide an override or if the {@code sqlTypeDescriptor} doe not allow itself to be
	 * remapped, then this method simply returns the original passed {@code sqlTypeDescriptor}
	 *
	 * @param sqlTypeDescriptor The {@link SqlTypeDescriptor} to override
	 * @return The {@link SqlTypeDescriptor} that should be used for this dialect;
	 *         if there is no override, then original {@code sqlTypeDescriptor} is returned.
	 * @throws IllegalArgumentException if {@code sqlTypeDescriptor} is null.
	 *
	 * @see #getSqlTypeDescriptorOverride
	 */
	public SqlTypeDescriptor remapSqlTypeDescriptor(SqlTypeDescriptor sqlTypeDescriptor) {
		if ( sqlTypeDescriptor == null ) {
			throw new IllegalArgumentException( "sqlTypeDescriptor is null" );
		}
		if ( ! sqlTypeDescriptor.canBeRemapped() ) {
			return sqlTypeDescriptor;
		}

		final SqlTypeDescriptor overridden = getSqlTypeDescriptorOverride( sqlTypeDescriptor.getSqlType() );
		return overridden == null ? sqlTypeDescriptor : overridden;
	}

	/**
	 * Returns the {@link SqlTypeDescriptor} that should be used to handle the given JDBC type code.  Returns
	 * {@code null} if there is no override.
	 *
	 * @param sqlCode A {@link Types} constant indicating the SQL column type
	 * @return The {@link SqlTypeDescriptor} to use as an override, or {@code null} if there is no override.
	 */
	protected SqlTypeDescriptor getSqlTypeDescriptorOverride(int sqlCode) {
		SqlTypeDescriptor descriptor;
		switch ( sqlCode ) {
			case Types.CLOB: {
				descriptor = useInputStreamToInsertBlob() ? ClobTypeDescriptor.STREAM_BINDING : null;
				break;
			}
			default: {
				descriptor = null;
				break;
			}
		}
		return descriptor;
	}

	/**
	 * The legacy behavior of Hibernate.  LOBs are not processed by merge
	 */
	@SuppressWarnings( {"UnusedDeclaration"})
	protected static final LobMergeStrategy LEGACY_LOB_MERGE_STRATEGY = new LobMergeStrategy() {
		@Override
		public Blob mergeBlob(Blob original, Blob target, SessionImplementor session) {
			return target;
		}

		@Override
		public Clob mergeClob(Clob original, Clob target, SessionImplementor session) {
			return target;
		}

		@Override
		public NClob mergeNClob(NClob original, NClob target, SessionImplementor session) {
			return target;
		}
	};

	/**
	 * Merge strategy based on transferring contents based on streams.
	 */
	@SuppressWarnings( {"UnusedDeclaration"})
	protected static final LobMergeStrategy STREAM_XFER_LOB_MERGE_STRATEGY = new LobMergeStrategy() {
		@Override
		public Blob mergeBlob(Blob original, Blob target, SessionImplementor session) {
			if ( original != target ) {
				try {
					// the BLOB just read during the load phase of merge
					final OutputStream connectedStream = target.setBinaryStream( 1L );
					// the BLOB from the detached state
					final InputStream detachedStream = original.getBinaryStream();
					StreamCopier.copy( detachedStream, connectedStream );
					return target;
				}
				catch (SQLException e ) {
					throw session.getFactory().getSQLExceptionHelper().convert( e, "unable to merge BLOB data" );
				}
			}
			else {
				return NEW_LOCATOR_LOB_MERGE_STRATEGY.mergeBlob( original, target, session );
			}
		}

		@Override
		public Clob mergeClob(Clob original, Clob target, SessionImplementor session) {
			if ( original != target ) {
				try {
					// the CLOB just read during the load phase of merge
					final OutputStream connectedStream = target.setAsciiStream( 1L );
					// the CLOB from the detached state
					final InputStream detachedStream = original.getAsciiStream();
					StreamCopier.copy( detachedStream, connectedStream );
					return target;
				}
				catch (SQLException e ) {
					throw session.getFactory().getSQLExceptionHelper().convert( e, "unable to merge CLOB data" );
				}
			}
			else {
				return NEW_LOCATOR_LOB_MERGE_STRATEGY.mergeClob( original, target, session );
			}
		}

		@Override
		public NClob mergeNClob(NClob original, NClob target, SessionImplementor session) {
			if ( original != target ) {
				try {
					// the NCLOB just read during the load phase of merge
					final OutputStream connectedStream = target.setAsciiStream( 1L );
					// the NCLOB from the detached state
					final InputStream detachedStream = original.getAsciiStream();
					StreamCopier.copy( detachedStream, connectedStream );
					return target;
				}
				catch (SQLException e ) {
					throw session.getFactory().getSQLExceptionHelper().convert( e, "unable to merge NCLOB data" );
				}
			}
			else {
				return NEW_LOCATOR_LOB_MERGE_STRATEGY.mergeNClob( original, target, session );
			}
		}
	};

	/**
	 * Merge strategy based on creating a new LOB locator.
	 */
	protected static final LobMergeStrategy NEW_LOCATOR_LOB_MERGE_STRATEGY = new LobMergeStrategy() {
		@Override
		public Blob mergeBlob(Blob original, Blob target, SessionImplementor session) {
			if ( original == null && target == null ) {
				return null;
			}
			try {
				final LobCreator lobCreator = session.getFactory().getServiceRegistry().getService( JdbcServices.class ).getLobCreator(
						session
				);
				return original == null
						? lobCreator.createBlob( ArrayHelper.EMPTY_BYTE_ARRAY )
						: lobCreator.createBlob( original.getBinaryStream(), original.length() );
			}
			catch (SQLException e) {
				throw session.getFactory().getSQLExceptionHelper().convert( e, "unable to merge BLOB data" );
			}
		}

		@Override
		public Clob mergeClob(Clob original, Clob target, SessionImplementor session) {
			if ( original == null && target == null ) {
				return null;
			}
			try {
				final LobCreator lobCreator = session.getFactory().getServiceRegistry().getService( JdbcServices.class ).getLobCreator( session );
				return original == null
						? lobCreator.createClob( "" )
						: lobCreator.createClob( original.getCharacterStream(), original.length() );
			}
			catch (SQLException e) {
				throw session.getFactory().getSQLExceptionHelper().convert( e, "unable to merge CLOB data" );
			}
		}

		@Override
		public NClob mergeNClob(NClob original, NClob target, SessionImplementor session) {
			if ( original == null && target == null ) {
				return null;
			}
			try {
				final LobCreator lobCreator = session.getFactory().getServiceRegistry().getService( JdbcServices.class ).getLobCreator( session );
				return original == null
						? lobCreator.createNClob( "" )
						: lobCreator.createNClob( original.getCharacterStream(), original.length() );
			}
			catch (SQLException e) {
				throw session.getFactory().getSQLExceptionHelper().convert( e, "unable to merge NCLOB data" );
			}
		}
	};

	public LobMergeStrategy getLobMergeStrategy() {
		return NEW_LOCATOR_LOB_MERGE_STRATEGY;
	}


	// hibernate type mapping support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Get the name of the Hibernate {@link org.hibernate.type.Type} associated with the given
	 * {@link java.sql.Types} type code.
	 *
	 * @param code The {@link java.sql.Types} type code
	 * @return The Hibernate {@link org.hibernate.type.Type} name.
	 * @throws HibernateException If no mapping was specified for that type.
	 */
	@SuppressWarnings( {"UnusedDeclaration"})
	public String getHibernateTypeName(int code) throws HibernateException {
		final String result = hibernateTypeNames.get( code );
		if ( result == null ) {
			throw new HibernateException( "No Hibernate type mapping for java.sql.Types code: " + code );
		}
		return result;
	}

	/**
	 * Get the name of the Hibernate {@link org.hibernate.type.Type} associated
	 * with the given {@link java.sql.Types} typecode with the given storage
	 * specification parameters.
	 *
	 * @param code The {@link java.sql.Types} typecode
	 * @param length The datatype length
	 * @param precision The datatype precision
	 * @param scale The datatype scale
	 * @return The Hibernate {@link org.hibernate.type.Type} name.
	 * @throws HibernateException If no mapping was specified for that type.
	 */
	public String getHibernateTypeName(int code, int length, int precision, int scale) throws HibernateException {
		final String result = hibernateTypeNames.get( code, length, precision, scale );
		if ( result == null ) {
			throw new HibernateException(
					String.format(
							"No Hibernate type mapping for type [code=%s, length=%s]",
							code,
							length
					)
			);
		}
		return result;
	}

	/**
	 * Registers a Hibernate {@link org.hibernate.type.Type} name for the given
	 * {@link java.sql.Types} type code and maximum column length.
	 *
	 * @param code The {@link java.sql.Types} typecode
	 * @param capacity The maximum length of database type
	 * @param name The Hibernate {@link org.hibernate.type.Type} name
	 */
	protected void registerHibernateType(int code, long capacity, String name) {
		hibernateTypeNames.put( code, capacity, name);
	}

	/**
	 * Registers a Hibernate {@link org.hibernate.type.Type} name for the given
	 * {@link java.sql.Types} type code.
	 *
	 * @param code The {@link java.sql.Types} typecode
	 * @param name The Hibernate {@link org.hibernate.type.Type} name
	 */
	protected void registerHibernateType(int code, String name) {
		hibernateTypeNames.put( code, name);
	}


	// function support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	protected void registerFunction(String name, SQLFunction function) {
		// HHH-7721: SQLFunctionRegistry expects all lowercase.  Enforce,
		// just in case a user's customer dialect uses mixed cases.
		sqlFunctions.put( name.toLowerCase( Locale.ROOT ), function );
	}

	/**
	 * Retrieves a map of the dialect's registered functions
	 * (functionName => {@link org.hibernate.dialect.function.SQLFunction}).
	 *
	 * @return The map of registered functions.
	 */
	public final Map<String, SQLFunction> getFunctions() {
		return sqlFunctions;
	}


	// native identifier generation ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * The class (which implements {@link org.hibernate.id.IdentifierGenerator})
	 * which acts as this dialects native generation strategy.
	 * <p/>
	 * Comes into play whenever the user specifies the native generator.
	 *
	 * @return The native generator class.
	 */
	public Class getNativeIdentifierGeneratorClass() {
		if ( supportsIdentityColumns() ) {
			return IdentityGenerator.class;
		}
		else if ( supportsSequences() ) {
			return SequenceGenerator.class;
		}
		else {
			return SequenceStyleGenerator.class;
		}
	}


	// IDENTITY support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Does this dialect support identity column key generation?
	 *
	 * @return True if IDENTITY columns are supported; false otherwise.
	 */
	public boolean supportsIdentityColumns() {
		return false;
	}

	/**
	 * Does the dialect support some form of inserting and selecting
	 * the generated IDENTITY value all in the same statement.
	 *
	 * @return True if the dialect supports selecting the just
	 * generated IDENTITY in the insert statement.
	 */
	public boolean supportsInsertSelectIdentity() {
		return false;
	}

	/**
	 * Whether this dialect have an Identity clause added to the data type or a
	 * completely separate identity data type
	 *
	 * @return boolean
	 */
	public boolean hasDataTypeInIdentityColumn() {
		return true;
	}

	/**
	 * Provided we {@link #supportsInsertSelectIdentity}, then attach the
	 * "select identity" clause to the  insert statement.
	 *  <p/>
	 * Note, if {@link #supportsInsertSelectIdentity} == false then
	 * the insert-string should be returned without modification.
	 *
	 * @param insertString The insert command
	 * @return The insert command with any necessary identity select
	 * clause attached.
	 */
	public String appendIdentitySelectToInsert(String insertString) {
		return insertString;
	}

	/**
	 * Get the select command to use to retrieve the last generated IDENTITY
	 * value for a particular table
	 *
	 * @param table The table into which the insert was done
	 * @param column The PK column.
	 * @param type The {@link java.sql.Types} type code.
	 * @return The appropriate select command
	 * @throws MappingException If IDENTITY generation is not supported.
	 */
	public String getIdentitySelectString(String table, String column, int type) throws MappingException {
		return getIdentitySelectString();
	}

	/**
	 * Get the select command to use to retrieve the last generated IDENTITY
	 * value.
	 *
	 * @return The appropriate select command
	 * @throws MappingException If IDENTITY generation is not supported.
	 */
	protected String getIdentitySelectString() throws MappingException {
		throw new MappingException( getClass().getName() + " does not support identity key generation" );
	}

	/**
	 * The syntax used during DDL to define a column as being an IDENTITY of
	 * a particular type.
	 *
	 * @param type The {@link java.sql.Types} type code.
	 * @return The appropriate DDL fragment.
	 * @throws MappingException If IDENTITY generation is not supported.
	 */
	public String getIdentityColumnString(int type) throws MappingException {
		return getIdentityColumnString();
	}

	/**
	 * The syntax used during DDL to define a column as being an IDENTITY.
	 *
	 * @return The appropriate DDL fragment.
	 * @throws MappingException If IDENTITY generation is not supported.
	 */
	protected String getIdentityColumnString() throws MappingException {
		throw new MappingException( getClass().getName() + " does not support identity key generation" );
	}

	/**
	 * The keyword used to insert a generated value into an identity column (or null).
	 * Need if the dialect does not support inserts that specify no column values.
	 *
	 * @return The appropriate keyword.
	 */
	public String getIdentityInsertString() {
		return null;
	}


	// SEQUENCE support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Does this dialect support sequences?
	 *
	 * @return True if sequences supported; false otherwise.
	 */
	public boolean supportsSequences() {
		return false;
	}

	/**
	 * Does this dialect support "pooled" sequences.  Not aware of a better
	 * name for this.  Essentially can we specify the initial and increment values?
	 *
	 * @return True if such "pooled" sequences are supported; false otherwise.
	 * @see #getCreateSequenceStrings(String, int, int)
	 * @see #getCreateSequenceString(String, int, int)
	 */
	public boolean supportsPooledSequences() {
		return false;
	}

	/**
	 * Generate the appropriate select statement to to retrieve the next value
	 * of a sequence.
	 * <p/>
	 * This should be a "stand alone" select statement.
	 *
	 * @param sequenceName the name of the sequence
	 * @return String The "nextval" select string.
	 * @throws MappingException If sequences are not supported.
	 */
	public String getSequenceNextValString(String sequenceName) throws MappingException {
		throw new MappingException( getClass().getName() + " does not support sequences" );
	}

	/**
	 * Generate the select expression fragment that will retrieve the next
	 * value of a sequence as part of another (typically DML) statement.
	 * <p/>
	 * This differs from {@link #getSequenceNextValString(String)} in that this
	 * should return an expression usable within another statement.
	 *
	 * @param sequenceName the name of the sequence
	 * @return The "nextval" fragment.
	 * @throws MappingException If sequences are not supported.
	 */
	public String getSelectSequenceNextValString(String sequenceName) throws MappingException {
		throw new MappingException( getClass().getName() + " does not support sequences" );
	}

	/**
	 * The multiline script used to create a sequence.
	 *
	 * @param sequenceName The name of the sequence
	 * @return The sequence creation commands
	 * @throws MappingException If sequences are not supported.
	 * @deprecated Use {@link #getCreateSequenceString(String, int, int)} instead
	 */
	@Deprecated
	public String[] getCreateSequenceStrings(String sequenceName) throws MappingException {
		return new String[] { getCreateSequenceString( sequenceName ) };
	}

	/**
	 * An optional multi-line form for databases which {@link #supportsPooledSequences()}.
	 *
	 * @param sequenceName The name of the sequence
	 * @param initialValue The initial value to apply to 'create sequence' statement
	 * @param incrementSize The increment value to apply to 'create sequence' statement
	 * @return The sequence creation commands
	 * @throws MappingException If sequences are not supported.
	 */
	public String[] getCreateSequenceStrings(String sequenceName, int initialValue, int incrementSize) throws MappingException {
		return new String[] { getCreateSequenceString( sequenceName, initialValue, incrementSize ) };
	}

	/**
	 * Typically dialects which support sequences can create a sequence
	 * with a single command.  This is convenience form of
	 * {@link #getCreateSequenceStrings} to help facilitate that.
	 * <p/>
	 * Dialects which support sequences and can create a sequence in a
	 * single command need *only* override this method.  Dialects
	 * which support sequences but require multiple commands to create
	 * a sequence should instead override {@link #getCreateSequenceStrings}.
	 *
	 * @param sequenceName The name of the sequence
	 * @return The sequence creation command
	 * @throws MappingException If sequences are not supported.
	 */
	protected String getCreateSequenceString(String sequenceName) throws MappingException {
		throw new MappingException( getClass().getName() + " does not support sequences" );
	}

	/**
	 * Overloaded form of {@link #getCreateSequenceString(String)}, additionally
	 * taking the initial value and increment size to be applied to the sequence
	 * definition.
	 * </p>
	 * The default definition is to suffix {@link #getCreateSequenceString(String)}
	 * with the string: " start with {initialValue} increment by {incrementSize}" where
	 * {initialValue} and {incrementSize} are replacement placeholders.  Generally
	 * dialects should only need to override this method if different key phrases
	 * are used to apply the allocation information.
	 *
	 * @param sequenceName The name of the sequence
	 * @param initialValue The initial value to apply to 'create sequence' statement
	 * @param incrementSize The increment value to apply to 'create sequence' statement
	 * @return The sequence creation command
	 * @throws MappingException If sequences are not supported.
	 */
	protected String getCreateSequenceString(String sequenceName, int initialValue, int incrementSize) throws MappingException {
		if ( supportsPooledSequences() ) {
			return getCreateSequenceString( sequenceName ) + " start with " + initialValue + " increment by " + incrementSize;
		}
		throw new MappingException( getClass().getName() + " does not support pooled sequences" );
	}

	/**
	 * The multiline script used to drop a sequence.
	 *
	 * @param sequenceName The name of the sequence
	 * @return The sequence drop commands
	 * @throws MappingException If sequences are not supported.
	 */
	public String[] getDropSequenceStrings(String sequenceName) throws MappingException {
		return new String[]{getDropSequenceString( sequenceName )};
	}

	/**
	 * Typically dialects which support sequences can drop a sequence
	 * with a single command.  This is convenience form of
	 * {@link #getDropSequenceStrings} to help facilitate that.
	 * <p/>
	 * Dialects which support sequences and can drop a sequence in a
	 * single command need *only* override this method.  Dialects
	 * which support sequences but require multiple commands to drop
	 * a sequence should instead override {@link #getDropSequenceStrings}.
	 *
	 * @param sequenceName The name of the sequence
	 * @return The sequence drop commands
	 * @throws MappingException If sequences are not supported.
	 */
	protected String getDropSequenceString(String sequenceName) throws MappingException {
		throw new MappingException( getClass().getName() + " does not support sequences" );
	}

	/**
	 * Get the select command used retrieve the names of all sequences.
	 *
	 * @return The select command; or null if sequences are not supported.
	 * @see org.hibernate.tool.hbm2ddl.SchemaUpdate
	 */
	public String getQuerySequencesString() {
		return null;
	}

	public SequenceInformationExtractor getSequenceInformationExtractor() {
		if ( getQuerySequencesString() == null ) {
			return SequenceInformationExtractorNoOpImpl.INSTANCE;
		}
		else {
			return SequenceInformationExtractorLegacyImpl.INSTANCE;
		}
	}


	// GUID support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Get the command used to select a GUID from the underlying database.
	 * <p/>
	 * Optional operation.
	 *
	 * @return The appropriate command.
	 */
	public String getSelectGUIDString() {
		throw new UnsupportedOperationException( getClass().getName() + " does not support GUIDs" );
	}


	// limit/offset support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Returns the delegate managing LIMIT clause.
	 *
	 * @return LIMIT clause delegate.
	 */
	public LimitHandler getLimitHandler() {
		return new LegacyLimitHandler( this );
	}

	/**
	 * Does this dialect support some form of limiting query results
	 * via a SQL clause?
	 *
	 * @return True if this dialect supports some form of LIMIT.
	 * @deprecated {@link #getLimitHandler()} should be overridden instead.
	 */
	@Deprecated
	public boolean supportsLimit() {
		return false;
	}

	/**
	 * Does this dialect's LIMIT support (if any) additionally
	 * support specifying an offset?
	 *
	 * @return True if the dialect supports an offset within the limit support.
	 * @deprecated {@link #getLimitHandler()} should be overridden instead.
	 */
	@Deprecated
	public boolean supportsLimitOffset() {
		return supportsLimit();
	}

	/**
	 * Does this dialect support bind variables (i.e., prepared statement
	 * parameters) for its limit/offset?
	 *
	 * @return True if bind variables can be used; false otherwise.
	 * @deprecated {@link #getLimitHandler()} should be overridden instead.
	 */
	@Deprecated
	public boolean supportsVariableLimit() {
		return supportsLimit();
	}

	/**
	 * ANSI SQL defines the LIMIT clause to be in the form LIMIT offset, limit.
	 * Does this dialect require us to bind the parameters in reverse order?
	 *
	 * @return true if the correct order is limit, offset
	 * @deprecated {@link #getLimitHandler()} should be overridden instead.
	 */
	@Deprecated
	public boolean bindLimitParametersInReverseOrder() {
		return false;
	}

	/**
	 * Does the <tt>LIMIT</tt> clause come at the start of the
	 * <tt>SELECT</tt> statement, rather than at the end?
	 *
	 * @return true if limit parameters should come before other parameters
	 * @deprecated {@link #getLimitHandler()} should be overridden instead.
	 */
	@Deprecated
	public boolean bindLimitParametersFirst() {
		return false;
	}

	/**
	 * Does the <tt>LIMIT</tt> clause take a "maximum" row number instead
	 * of a total number of returned rows?
	 * <p/>
	 * This is easiest understood via an example.  Consider you have a table
	 * with 20 rows, but you only want to retrieve rows number 11 through 20.
	 * Generally, a limit with offset would say that the offset = 11 and the
	 * limit = 10 (we only want 10 rows at a time); this is specifying the
	 * total number of returned rows.  Some dialects require that we instead
	 * specify offset = 11 and limit = 20, where 20 is the "last" row we want
	 * relative to offset (i.e. total number of rows = 20 - 11 = 9)
	 * <p/>
	 * So essentially, is limit relative from offset?  Or is limit absolute?
	 *
	 * @return True if limit is relative from offset; false otherwise.
	 * @deprecated {@link #getLimitHandler()} should be overridden instead.
	 */
	@Deprecated
	public boolean useMaxForLimit() {
		return false;
	}

	/**
	 * Generally, if there is no limit applied to a Hibernate query we do not apply any limits
	 * to the SQL query.  This option forces that the limit be written to the SQL query.
	 *
	 * @return True to force limit into SQL query even if none specified in Hibernate query; false otherwise.
	 * @deprecated {@link #getLimitHandler()} should be overridden instead.
	 */
	@Deprecated
	public boolean forceLimitUsage() {
		return false;
	}

	/**
	 * Given a limit and an offset, apply the limit clause to the query.
	 *
	 * @param query The query to which to apply the limit.
	 * @param offset The offset of the limit
	 * @param limit The limit of the limit ;)
	 * @return The modified query statement with the limit applied.
	 * @deprecated {@link #getLimitHandler()} should be overridden instead.
	 */
	@Deprecated
	public String getLimitString(String query, int offset, int limit) {
		return getLimitString( query, ( offset > 0 || forceLimitUsage() )  );
	}

	/**
	 * Apply s limit clause to the query.
	 * <p/>
	 * Typically dialects utilize {@link #supportsVariableLimit() variable}
	 * limit clauses when they support limits.  Thus, when building the
	 * select command we do not actually need to know the limit or the offest
	 * since we will just be using placeholders.
	 * <p/>
	 * Here we do still pass along whether or not an offset was specified
	 * so that dialects not supporting offsets can generate proper exceptions.
	 * In general, dialects will override one or the other of this method and
	 * {@link #getLimitString(String, int, int)}.
	 *
	 * @param query The query to which to apply the limit.
	 * @param hasOffset Is the query requesting an offset?
	 * @return the modified SQL
	 * @deprecated {@link #getLimitHandler()} should be overridden instead.
	 */
	@Deprecated
	protected String getLimitString(String query, boolean hasOffset) {
		throw new UnsupportedOperationException( "Paged queries not supported by " + getClass().getName());
	}

	/**
	 * Hibernate APIs explicitly state that setFirstResult() should be a zero-based offset. Here we allow the
	 * Dialect a chance to convert that value based on what the underlying db or driver will expect.
	 * <p/>
	 * NOTE: what gets passed into {@link #getLimitString(String,int,int)} is the zero-based offset.  Dialects which
	 * do not {@link #supportsVariableLimit} should take care to perform any needed first-row-conversion calls prior
	 * to injecting the limit values into the SQL string.
	 *
	 * @param zeroBasedFirstResult The user-supplied, zero-based first row offset.
	 * @return The corresponding db/dialect specific offset.
	 * @see org.hibernate.Query#setFirstResult
	 * @see org.hibernate.Criteria#setFirstResult
	 * @deprecated {@link #getLimitHandler()} should be overridden instead.
	 */
	@Deprecated
	public int convertToFirstRowValue(int zeroBasedFirstResult) {
		return zeroBasedFirstResult;
	}


	// lock acquisition support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Informational metadata about whether this dialect is known to support
	 * specifying timeouts for requested lock acquisitions.
	 *
	 * @return True is this dialect supports specifying lock timeouts.
	 */
	public boolean supportsLockTimeouts() {
		return true;

	}

	/**
	 * If this dialect supports specifying lock timeouts, are those timeouts
	 * rendered into the <tt>SQL</tt> string as parameters.  The implication
	 * is that Hibernate will need to bind the timeout value as a parameter
	 * in the {@link java.sql.PreparedStatement}.  If true, the param position
	 * is always handled as the last parameter; if the dialect specifies the
	 * lock timeout elsewhere in the <tt>SQL</tt> statement then the timeout
	 * value should be directly rendered into the statement and this method
	 * should return false.
	 *
	 * @return True if the lock timeout is rendered into the <tt>SQL</tt>
	 * string as a parameter; false otherwise.
	 */
	public boolean isLockTimeoutParameterized() {
		return false;
	}

	/**
	 * Get a strategy instance which knows how to acquire a database-level lock
	 * of the specified mode for this dialect.
	 *
	 * @param lockable The persister for the entity to be locked.
	 * @param lockMode The type of lock to be acquired.
	 * @return The appropriate locking strategy.
	 * @since 3.2
	 */
	public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
		switch ( lockMode ) {
			case PESSIMISTIC_FORCE_INCREMENT:
				return new PessimisticForceIncrementLockingStrategy( lockable, lockMode );
			case PESSIMISTIC_WRITE:
				return new PessimisticWriteSelectLockingStrategy( lockable, lockMode );
			case PESSIMISTIC_READ:
				return new PessimisticReadSelectLockingStrategy( lockable, lockMode );
			case OPTIMISTIC:
				return new OptimisticLockingStrategy( lockable, lockMode );
			case OPTIMISTIC_FORCE_INCREMENT:
				return new OptimisticForceIncrementLockingStrategy( lockable, lockMode );
			default:
				return new SelectLockingStrategy( lockable, lockMode );
		}
	}

	/**
	 * Given LockOptions (lockMode, timeout), determine the appropriate for update fragment to use.
	 *
	 * @param lockOptions contains the lock mode to apply.
	 * @return The appropriate for update fragment.
	 */
	public String getForUpdateString(LockOptions lockOptions) {
		final LockMode lockMode = lockOptions.getLockMode();
		return getForUpdateString( lockMode, lockOptions.getTimeOut() );
	}

	@SuppressWarnings( {"deprecation"})
	private String getForUpdateString(LockMode lockMode, int timeout){
		switch ( lockMode ) {
			case UPGRADE:
				return getForUpdateString();
			case PESSIMISTIC_READ:
				return getReadLockString( timeout );
			case PESSIMISTIC_WRITE:
				return getWriteLockString( timeout );
			case UPGRADE_NOWAIT:
			case FORCE:
			case PESSIMISTIC_FORCE_INCREMENT:
				return getForUpdateNowaitString();
			case UPGRADE_SKIPLOCKED:
				return getForUpdateSkipLockedString();
			default:
				return "";
		}
	}

	/**
	 * Given a lock mode, determine the appropriate for update fragment to use.
	 *
	 * @param lockMode The lock mode to apply.
	 * @return The appropriate for update fragment.
	 */
	public String getForUpdateString(LockMode lockMode) {
		return getForUpdateString( lockMode, LockOptions.WAIT_FOREVER );
	}

	/**
	 * Get the string to append to SELECT statements to acquire locks
	 * for this dialect.
	 *
	 * @return The appropriate <tt>FOR UPDATE</tt> clause string.
	 */
	public String getForUpdateString() {
		return " for update";
	}

	/**
	 * Get the string to append to SELECT statements to acquire WRITE locks
	 * for this dialect.  Location of the of the returned string is treated
	 * the same as getForUpdateString.
	 *
	 * @param timeout in milliseconds, -1 for indefinite wait and 0 for no wait.
	 * @return The appropriate <tt>LOCK</tt> clause string.
	 */
	public String getWriteLockString(int timeout) {
		return getForUpdateString();
	}

	/**
	 * Get the string to append to SELECT statements to acquire WRITE locks
	 * for this dialect.  Location of the of the returned string is treated
	 * the same as getForUpdateString.
	 *
	 * @param timeout in milliseconds, -1 for indefinite wait and 0 for no wait.
	 * @return The appropriate <tt>LOCK</tt> clause string.
	 */
	public String getReadLockString(int timeout) {
		return getForUpdateString();
	}


	/**
	 * Is <tt>FOR UPDATE OF</tt> syntax supported?
	 *
	 * @return True if the database supports <tt>FOR UPDATE OF</tt> syntax;
	 * false otherwise.
	 */
	public boolean forUpdateOfColumns() {
		// by default we report no support
		return false;
	}

	/**
	 * Does this dialect support <tt>FOR UPDATE</tt> in conjunction with
	 * outer joined rows?
	 *
	 * @return True if outer joined rows can be locked via <tt>FOR UPDATE</tt>.
	 */
	public boolean supportsOuterJoinForUpdate() {
		return true;
	}

	/**
	 * Get the <tt>FOR UPDATE OF column_list</tt> fragment appropriate for this
	 * dialect given the aliases of the columns to be write locked.
	 *
	 * @param aliases The columns to be write locked.
	 * @return The appropriate <tt>FOR UPDATE OF column_list</tt> clause string.
	 */
	public String getForUpdateString(String aliases) {
		// by default we simply return the getForUpdateString() result since
		// the default is to say no support for "FOR UPDATE OF ..."
		return getForUpdateString();
	}

	/**
	 * Get the <tt>FOR UPDATE OF column_list</tt> fragment appropriate for this
	 * dialect given the aliases of the columns to be write locked.
	 *
	 * @param aliases The columns to be write locked.
	 * @param lockOptions the lock options to apply
	 * @return The appropriate <tt>FOR UPDATE OF column_list</tt> clause string.
	 */
	@SuppressWarnings({"unchecked", "UnusedParameters"})
	public String getForUpdateString(String aliases, LockOptions lockOptions) {
		LockMode lockMode = lockOptions.getLockMode();
		final Iterator<Map.Entry<String, LockMode>> itr = lockOptions.getAliasLockIterator();
		while ( itr.hasNext() ) {
			// seek the highest lock mode
			final Map.Entry<String, LockMode>entry = itr.next();
			final LockMode lm = entry.getValue();
			if ( lm.greaterThan( lockMode ) ) {
				lockMode = lm;
			}
		}
		lockOptions.setLockMode( lockMode );
		return getForUpdateString( lockOptions );
	}

	/**
	 * Retrieves the <tt>FOR UPDATE NOWAIT</tt> syntax specific to this dialect.
	 *
	 * @return The appropriate <tt>FOR UPDATE NOWAIT</tt> clause string.
	 */
	public String getForUpdateNowaitString() {
		// by default we report no support for NOWAIT lock semantics
		return getForUpdateString();
	}

	/**
	 * Retrieves the <tt>FOR UPDATE SKIP LOCKED</tt> syntax specific to this dialect.
	 *
	 * @return The appropriate <tt>FOR UPDATE SKIP LOCKED</tt> clause string.
	 */
	public String getForUpdateSkipLockedString() {
		// by default we report no support for SKIP_LOCKED lock semantics
		return getForUpdateString();
	}

	/**
	 * Get the <tt>FOR UPDATE OF column_list NOWAIT</tt> fragment appropriate
	 * for this dialect given the aliases of the columns to be write locked.
	 *
	 * @param aliases The columns to be write locked.
	 * @return The appropriate <tt>FOR UPDATE OF colunm_list NOWAIT</tt> clause string.
	 */
	public String getForUpdateNowaitString(String aliases) {
		return getForUpdateString( aliases );
	}

	/**
	 * Get the <tt>FOR UPDATE OF column_list SKIP LOCKED</tt> fragment appropriate
	 * for this dialect given the aliases of the columns to be write locked.
	 *
	 * @param aliases The columns to be write locked.
	 * @return The appropriate <tt>FOR UPDATE colunm_list SKIP LOCKED</tt> clause string.
	 */
	public String getForUpdateSkipLockedString(String aliases) {
		return getForUpdateString( aliases );
	}

	/**
	 * Some dialects support an alternative means to <tt>SELECT FOR UPDATE</tt>,
	 * whereby a "lock hint" is appends to the table name in the from clause.
	 * <p/>
	 * contributed by <a href="http://sourceforge.net/users/heschulz">Helge Schulz</a>
	 *
	 * @param mode The lock mode to apply
	 * @param tableName The name of the table to which to apply the lock hint.
	 * @return The table with any required lock hints.
	 * @deprecated use {@code appendLockHint(LockOptions,String)} instead
	 */
	@Deprecated
	public String appendLockHint(LockMode mode, String tableName) {
		return appendLockHint( new LockOptions( mode ), tableName );
	}
	/**
	 * Some dialects support an alternative means to <tt>SELECT FOR UPDATE</tt>,
	 * whereby a "lock hint" is appends to the table name in the from clause.
	 * <p/>
	 * contributed by <a href="http://sourceforge.net/users/heschulz">Helge Schulz</a>
	 *
	 * @param lockOptions The lock options to apply
	 * @param tableName The name of the table to which to apply the lock hint.
	 * @return The table with any required lock hints.
	 */
	public String appendLockHint(LockOptions lockOptions, String tableName){
		return tableName;
	}

	/**
	 * Modifies the given SQL by applying the appropriate updates for the specified
	 * lock modes and key columns.
	 * <p/>
	 * The behavior here is that of an ANSI SQL <tt>SELECT FOR UPDATE</tt>.  This
	 * method is really intended to allow dialects which do not support
	 * <tt>SELECT FOR UPDATE</tt> to achieve this in their own fashion.
	 *
	 * @param sql the SQL string to modify
	 * @param aliasedLockOptions lock options indexed by aliased table names.
	 * @param keyColumnNames a map of key columns indexed by aliased table names.
	 * @return the modified SQL string.
	 */
	public String applyLocksToSql(String sql, LockOptions aliasedLockOptions, Map<String, String[]> keyColumnNames) {
		return sql + new ForUpdateFragment( this, aliasedLockOptions, keyColumnNames ).toFragmentString();
	}


	// table support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Command used to create a table.
	 *
	 * @return The command used to create a table.
	 */
	public String getCreateTableString() {
		return "create table";
	}

	/**
	 * Slight variation on {@link #getCreateTableString}.  Here, we have the
	 * command used to create a table when there is no primary key and
	 * duplicate rows are expected.
	 * <p/>
	 * Most databases do not care about the distinction; originally added for
	 * Teradata support which does care.
	 *
	 * @return The command used to create a multiset table.
	 */
	public String getCreateMultisetTableString() {
		return getCreateTableString();
	}

	public MultiTableBulkIdStrategy getDefaultMultiTableBulkIdStrategy() {
		return new PersistentTableBulkIdStrategy();
	}

	// callable statement support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Registers a parameter (either OUT, or the new REF_CURSOR param type available in Java 8) capable of
	 * returning {@link java.sql.ResultSet} *by position*.  Pre-Java 8, registering such ResultSet-returning
	 * parameters varied greatly across database and drivers; hence its inclusion as part of the Dialect contract.
	 *
	 * @param statement The callable statement.
	 * @param position The bind position at which to register the output param.
	 *
	 * @return The number of (contiguous) bind positions used.
	 *
	 * @throws SQLException Indicates problems registering the param.
	 */
	public int registerResultSetOutParameter(CallableStatement statement, int position) throws SQLException {
		throw new UnsupportedOperationException(
				getClass().getName() +
						" does not support resultsets via stored procedures"
		);
	}

	/**
	 * Registers a parameter (either OUT, or the new REF_CURSOR param type available in Java 8) capable of
	 * returning {@link java.sql.ResultSet} *by name*.  Pre-Java 8, registering such ResultSet-returning
	 * parameters varied greatly across database and drivers; hence its inclusion as part of the Dialect contract.
	 *
	 * @param statement The callable statement.
	 * @param name The parameter name (for drivers which support named parameters).
	 *
	 * @return The number of (contiguous) bind positions used.
	 *
	 * @throws SQLException Indicates problems registering the param.
	 */
	@SuppressWarnings("UnusedParameters")
	public int registerResultSetOutParameter(CallableStatement statement, String name) throws SQLException {
		throw new UnsupportedOperationException(
				getClass().getName() +
						" does not support resultsets via stored procedures"
		);
	}

	/**
	 * Given a callable statement previously processed by {@link #registerResultSetOutParameter},
	 * extract the {@link java.sql.ResultSet} from the OUT parameter.
	 *
	 * @param statement The callable statement.
	 * @return The extracted result set.
	 * @throws SQLException Indicates problems extracting the result set.
	 */
	public ResultSet getResultSet(CallableStatement statement) throws SQLException {
		throw new UnsupportedOperationException(
				getClass().getName() + " does not support resultsets via stored procedures"
		);
	}

	/**
	 * Given a callable statement previously processed by {@link #registerResultSetOutParameter},
	 * extract the {@link java.sql.ResultSet}.
	 *
	 * @param statement The callable statement.
	 * @param position The bind position at which to register the output param.
	 *
	 * @return The extracted result set.
	 *
	 * @throws SQLException Indicates problems extracting the result set.
	 */
	@SuppressWarnings("UnusedParameters")
	public ResultSet getResultSet(CallableStatement statement, int position) throws SQLException {
		throw new UnsupportedOperationException(
				getClass().getName() + " does not support resultsets via stored procedures"
		);
	}

	/**
	 * Given a callable statement previously processed by {@link #registerResultSetOutParameter},
	 * extract the {@link java.sql.ResultSet} from the OUT parameter.
	 *
	 * @param statement The callable statement.
	 * @param name The parameter name (for drivers which support named parameters).
	 *
	 * @return The extracted result set.
	 *
	 * @throws SQLException Indicates problems extracting the result set.
	 */
	@SuppressWarnings("UnusedParameters")
	public ResultSet getResultSet(CallableStatement statement, String name) throws SQLException {
		throw new UnsupportedOperationException(
				getClass().getName() + " does not support resultsets via stored procedures"
		);
	}

	// current timestamp support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Does this dialect support a way to retrieve the database's current
	 * timestamp value?
	 *
	 * @return True if the current timestamp can be retrieved; false otherwise.
	 */
	public boolean supportsCurrentTimestampSelection() {
		return false;
	}

	/**
	 * Should the value returned by {@link #getCurrentTimestampSelectString}
	 * be treated as callable.  Typically this indicates that JDBC escape
	 * syntax is being used...
	 *
	 * @return True if the {@link #getCurrentTimestampSelectString} return
	 * is callable; false otherwise.
	 */
	public boolean isCurrentTimestampSelectStringCallable() {
		throw new UnsupportedOperationException( "Database not known to define a current timestamp function" );
	}

	/**
	 * Retrieve the command used to retrieve the current timestamp from the
	 * database.
	 *
	 * @return The command.
	 */
	public String getCurrentTimestampSelectString() {
		throw new UnsupportedOperationException( "Database not known to define a current timestamp function" );
	}

	/**
	 * The name of the database-specific SQL function for retrieving the
	 * current timestamp.
	 *
	 * @return The function name.
	 */
	public String getCurrentTimestampSQLFunctionName() {
		// the standard SQL function name is current_timestamp...
		return "current_timestamp";
	}


	// SQLException support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Build an instance of the SQLExceptionConverter preferred by this dialect for
	 * converting SQLExceptions into Hibernate's JDBCException hierarchy.
	 * <p/>
	 * The preferred method is to not override this method; if possible,
	 * {@link #buildSQLExceptionConversionDelegate()} should be overridden
	 * instead.
	 *
	 * If this method is not overridden, the default SQLExceptionConverter
	 * implementation executes 3 SQLException converter delegates:
	 * <ol>
	 *     <li>a "static" delegate based on the JDBC 4 defined SQLException hierarchy;</li>
	 *     <li>the vendor-specific delegate returned by {@link #buildSQLExceptionConversionDelegate()};
	 *         (it is strongly recommended that specific Dialect implementations
	 *         override {@link #buildSQLExceptionConversionDelegate()})</li>
	 *     <li>a delegate that interprets SQLState codes for either X/Open or SQL-2003 codes,
	 *         depending on java.sql.DatabaseMetaData#getSQLStateType</li>
	 * </ol>
	 * <p/>
	 * If this method is overridden, it is strongly recommended that the
	 * returned {@link SQLExceptionConverter} interpret SQL errors based on
	 * vendor-specific error codes rather than the SQLState since the
	 * interpretation is more accurate when using vendor-specific ErrorCodes.
	 *
	 * @return The Dialect's preferred SQLExceptionConverter, or null to
	 * indicate that the default {@link SQLExceptionConverter} should be used.
	 *
	 * @see {@link #buildSQLExceptionConversionDelegate()}
	 * @deprecated {@link #buildSQLExceptionConversionDelegate()} should be
	 * overridden instead.
	 */
	@Deprecated
	public SQLExceptionConverter buildSQLExceptionConverter() {
		return null;
	}

	/**
	 * Build an instance of a {@link SQLExceptionConversionDelegate} for
	 * interpreting dialect-specific error or SQLState codes.
	 * <p/>
	 * When {@link #buildSQLExceptionConverter} returns null, the default 
	 * {@link SQLExceptionConverter} is used to interpret SQLState and
	 * error codes. If this method is overridden to return a non-null value,
	 * the default {@link SQLExceptionConverter} will use the returned
	 * {@link SQLExceptionConversionDelegate} in addition to the following 
	 * standard delegates:
	 * <ol>
	 *     <li>a "static" delegate based on the JDBC 4 defined SQLException hierarchy;</li>
	 *     <li>a delegate that interprets SQLState codes for either X/Open or SQL-2003 codes,
	 *         depending on java.sql.DatabaseMetaData#getSQLStateType</li>
	 * </ol>
	 * <p/>
	 * It is strongly recommended that specific Dialect implementations override this
	 * method, since interpretation of a SQL error is much more accurate when based on
	 * the a vendor-specific ErrorCode rather than the SQLState.
	 * <p/>
	 * Specific Dialects may override to return whatever is most appropriate for that vendor.
	 *
	 * @return The SQLExceptionConversionDelegate for this dialect
	 */
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return null;
	}

	private static final ViolatedConstraintNameExtracter EXTRACTER = new ViolatedConstraintNameExtracter() {
		public String extractConstraintName(SQLException sqle) {
			return null;
		}
	};

	public ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter() {
		return EXTRACTER;
	}


	// union subclass support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Given a {@link java.sql.Types} type code, determine an appropriate
	 * null value to use in a select clause.
	 * <p/>
	 * One thing to consider here is that certain databases might
	 * require proper casting for the nulls here since the select here
	 * will be part of a UNION/UNION ALL.
	 *
	 * @param sqlType The {@link java.sql.Types} type code.
	 * @return The appropriate select clause value fragment.
	 */
	public String getSelectClauseNullString(int sqlType) {
		return "null";
	}

	/**
	 * Does this dialect support UNION ALL, which is generally a faster
	 * variant of UNION?
	 *
	 * @return True if UNION ALL is supported; false otherwise.
	 */
	public boolean supportsUnionAll() {
		return false;
	}


	// miscellaneous support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


	/**
	 * Create a {@link org.hibernate.sql.JoinFragment} strategy responsible
	 * for handling this dialect's variations in how joins are handled.
	 *
	 * @return This dialect's {@link org.hibernate.sql.JoinFragment} strategy.
	 */
	public JoinFragment createOuterJoinFragment() {
		return new ANSIJoinFragment();
	}

	/**
	 * Create a {@link org.hibernate.sql.CaseFragment} strategy responsible
	 * for handling this dialect's variations in how CASE statements are
	 * handled.
	 *
	 * @return This dialect's {@link org.hibernate.sql.CaseFragment} strategy.
	 */
	public CaseFragment createCaseFragment() {
		return new ANSICaseFragment();
	}

	/**
	 * The fragment used to insert a row without specifying any column values.
	 * This is not possible on some databases.
	 *
	 * @return The appropriate empty values clause.
	 */
	public String getNoColumnsInsertString() {
		return "values ( )";
	}

	/**
	 * The name of the SQL function that transforms a string to
	 * lowercase
	 *
	 * @return The dialect-specific lowercase function.
	 */
	public String getLowercaseFunction() {
		return "lower";
	}

	/**
	 * The name of the SQL function that can do case insensitive <b>like</b> comparison.
	 *
	 * @return  The dialect-specific "case insensitive" like function.
	 */
	public String getCaseInsensitiveLike(){
		return "like";
	}

	/**
	 * Does this dialect support case insensitive LIKE restrictions?
	 *
	 * @return {@code true} if the underlying database supports case insensitive like comparison,
	 * {@code false} otherwise.  The default is {@code false}.
	 */
	public boolean supportsCaseInsensitiveLike(){
		return false;
	}

	/**
	 * Meant as a means for end users to affect the select strings being sent
	 * to the database and perhaps manipulate them in some fashion.
	 * <p/>
	 * The recommend approach is to instead use
	 * {@link org.hibernate.Interceptor#onPrepareStatement(String)}.
	 *
	 * @param select The select command
	 * @return The mutated select command, or the same as was passed in.
	 */
	public String transformSelectString(String select) {
		return select;
	}

	/**
	 * What is the maximum length Hibernate can use for generated aliases?
	 * <p/>
	 * The maximum here should account for the fact that Hibernate often needs to append "uniqueing" information
	 * to the end of generated aliases.  That "uniqueing" information will be added to the end of a identifier
	 * generated to the length specified here; so be sure to leave some room (generally speaking 5 positions will
	 * suffice).
	 *
	 * @return The maximum length.
	 */
	public int getMaxAliasLength() {
		return 10;
	}

	/**
	 * The SQL literal value to which this database maps boolean values.
	 *
	 * @param bool The boolean value
	 * @return The appropriate SQL literal.
	 */
	public String toBooleanValueString(boolean bool) {
		return bool ? "1" : "0";
	}


	// keyword support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	protected void registerKeyword(String word) {
		sqlKeywords.add( word );
	}

	/**
	 * @deprecated These are only ever used (if at all) from the code that handles identifier quoting.  So
	 * see {@link #buildIdentifierHelper} instead
	 */
	@Deprecated
	public Set<String> getKeywords() {
		return sqlKeywords;
	}

	/**
	 * Build the IdentifierHelper indicated by this Dialect for handling identifier conversions.
	 * Returning {@code null} is allowed and indicates that Hibernate should fallback to building a
	 * "standard" helper.  In the fallback path, any changes made to the IdentifierHelperBuilder
	 * during this call will still be incorporated into the built IdentifierHelper.
	 * <p/>
	 * The incoming builder will have the following set:<ul>
	 *     <li>{@link IdentifierHelperBuilder#isGloballyQuoteIdentifiers()}</li>
	 *     <li>{@link IdentifierHelperBuilder#getUnquotedCaseStrategy()} - initialized to UPPER</li>
	 *     <li>{@link IdentifierHelperBuilder#getQuotedCaseStrategy()} - initialized to MIXED</li>
	 * </ul>
	 * <p/>
	 * By default Hibernate will do the following:<ul>
	 *     <li>Call {@link IdentifierHelperBuilder#applyIdentifierCasing(DatabaseMetaData)}
	 *     <li>Call {@link IdentifierHelperBuilder#applyReservedWords(DatabaseMetaData)}
	 *     <li>Applies {@link AnsiSqlKeywords#sql2003()} as reserved words</li>
	 *     <li>Applies the {#link #sqlKeywords} collected here as reserved words</li>
	 *     <li>Applies the Dialect's NameQualifierSupport, if it defines one</li>
	 * </ul>
	 *
	 * @param builder A semi-configured IdentifierHelper builder.
	 * @param dbMetaData Access to the metadata returned from the driver if needed and if available.  WARNING: may be {@code null}
	 *
	 * @return The IdentifierHelper instance to use, or {@code null} to indicate Hibernate should use its fallback path
	 *
	 * @throws SQLException Accessing the DatabaseMetaData can throw it.  Just re-throw and Hibernate will handle.
	 *
	 * @see #getNameQualifierSupport()
	 */
	public IdentifierHelper buildIdentifierHelper(
			IdentifierHelperBuilder builder,
			DatabaseMetaData dbMetaData) throws SQLException {
		builder.applyIdentifierCasing( dbMetaData );

		builder.applyReservedWords( dbMetaData );
		builder.applyReservedWords( AnsiSqlKeywords.INSTANCE.sql2003() );
		builder.applyReservedWords( sqlKeywords );

		builder.setNameQualifierSupport( getNameQualifierSupport() );

		return builder.build();
	}


	// identifier quoting support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * The character specific to this dialect used to begin a quoted identifier.
	 *
	 * @return The dialect's specific open quote character.
	 */
	public char openQuote() {
		return '"';
	}

	/**
	 * The character specific to this dialect used to close a quoted identifier.
	 *
	 * @return The dialect's specific close quote character.
	 */
	public char closeQuote() {
		return '"';
	}

	/**
	 * Apply dialect-specific quoting.
	 * <p/>
	 * By default, the incoming value is checked to see if its first character
	 * is the back-tick (`).  If so, the dialect specific quoting is applied.
	 *
	 * @param name The value to be quoted.
	 * @return The quoted (or unmodified, if not starting with back-tick) value.
	 * @see #openQuote()
	 * @see #closeQuote()
	 */
	public final String quote(String name) {
		if ( name == null ) {
			return null;
		}

		if ( name.charAt( 0 ) == '`' ) {
			return openQuote() + name.substring( 1, name.length() - 1 ) + closeQuote();
		}
		else {
			return name;
		}
	}


	// DDL support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private StandardTableExporter tableExporter = new StandardTableExporter( this );
	private StandardSequenceExporter sequenceExporter = new StandardSequenceExporter( this );
	private StandardIndexExporter indexExporter = new StandardIndexExporter( this );
	private StandardForeignKeyExporter foreignKeyExporter = new StandardForeignKeyExporter( this );
	private StandardUniqueKeyExporter uniqueKeyExporter = new StandardUniqueKeyExporter( this );
	private StandardAuxiliaryDatabaseObjectExporter auxiliaryObjectExporter = new StandardAuxiliaryDatabaseObjectExporter( this );

	public Exporter<Table> getTableExporter() {
		return tableExporter;
	}

	public Exporter<Sequence> getSequenceExporter() {
		return sequenceExporter;
	}

	public Exporter<Index> getIndexExporter() {
		return indexExporter;
	}

	public Exporter<ForeignKey> getForeignKeyExporter() {
		return foreignKeyExporter;
	}

	public Exporter<Constraint> getUniqueKeyExporter() {
		return uniqueKeyExporter;
	}

	public Exporter<AuxiliaryDatabaseObject> getAuxiliaryDatabaseObjectExporter() {
		return auxiliaryObjectExporter;
	}

	/**
	 * Does this dialect support catalog creation?
	 *
	 * @return True if the dialect supports catalog creation; false otherwise.
	 */
	public boolean canCreateCatalog() {
		return false;
	}

	/**
	 * Get the SQL command used to create the named catalog
	 *
	 * @param catalogName The name of the catalog to be created.
	 *
	 * @return The creation commands
	 */
	public String[] getCreateCatalogCommand(String catalogName) {
		throw new UnsupportedOperationException( "No create catalog syntax supported by " + getClass().getName() );
	}

	/**
	 * Get the SQL command used to drop the named catalog
	 *
	 * @param catalogName The name of the catalog to be dropped.
	 *
	 * @return The drop commands
	 */
	public String[] getDropCatalogCommand(String catalogName) {
		throw new UnsupportedOperationException( "No drop catalog syntax supported by " + getClass().getName() );
	}

	/**
	 * Does this dialect support schema creation?
	 *
	 * @return True if the dialect supports schema creation; false otherwise.
	 */
	public boolean canCreateSchema() {
		return true;
	}

	/**
	 * Get the SQL command used to create the named schema
	 *
	 * @param schemaName The name of the schema to be created.
	 *
	 * @return The creation commands
	 */
	public String[] getCreateSchemaCommand(String schemaName) {
		return new String[] {"create schema " + schemaName};
	}

	/**
	 * Get the SQL command used to drop the named schema
	 *
	 * @param schemaName The name of the schema to be dropped.
	 *
	 * @return The drop commands
	 */
	public String[] getDropSchemaCommand(String schemaName) {
		return new String[] {"drop schema " + schemaName};
	}

	/**
	 * Get the SQL command used to retrieve the current schema name.  Works in conjunction
	 * with {@link #getSchemaNameResolver()}, unless the return from there does not need this
	 * information.  E.g., a custom impl might make use of the Java 1.7 addition of
	 * the {@link java.sql.Connection#getSchema()} method
	 *
	 * @return The current schema retrieval SQL
	 */
	public String getCurrentSchemaCommand() {
		return null;
	}

	/**
	 * Get the strategy for determining the schema name of a Connection
	 *
	 * @return The schema name resolver strategy
	 */
	public SchemaNameResolver getSchemaNameResolver() {
		return DefaultSchemaNameResolver.INSTANCE;
	}

	/**
	 * Does this dialect support the <tt>ALTER TABLE</tt> syntax?
	 *
	 * @return True if we support altering of tables; false otherwise.
	 */
	public boolean hasAlterTable() {
		return true;
	}

	/**
	 * Do we need to drop constraints before dropping tables in this dialect?
	 *
	 * @return True if constraints must be dropped prior to dropping
	 * the table; false otherwise.
	 */
	public boolean dropConstraints() {
		return true;
	}

	/**
	 * Do we need to qualify index names with the schema name?
	 *
	 * @return boolean
	 */
	public boolean qualifyIndexName() {
		return true;
	}

	/**
	 * The syntax used to add a column to a table (optional).
	 *
	 * @return The "add column" fragment.
	 */
	public String getAddColumnString() {
		throw new UnsupportedOperationException( "No add column syntax supported by " + getClass().getName() );
	}

	/**
	 * The syntax for the suffix used to add a column to a table (optional).
	 *
	 * @return The suffix "add column" fragment.
	 */
	public String getAddColumnSuffixString() {
		return "";
	}

	public String getDropForeignKeyString() {
		return " drop constraint ";
	}

	public String getTableTypeString() {
		// grrr... for differentiation of mysql storage engines
		return "";
	}

	/**
	 * The syntax used to add a foreign key constraint to a table.
	 *
	 * @param constraintName The FK constraint name.
	 * @param foreignKey The names of the columns comprising the FK
	 * @param referencedTable The table referenced by the FK
	 * @param primaryKey The explicit columns in the referencedTable referenced
	 * by this FK.
	 * @param referencesPrimaryKey if false, constraint should be
	 * explicit about which column names the constraint refers to
	 *
	 * @return the "add FK" fragment
	 */
	public String getAddForeignKeyConstraintString(
			String constraintName,
			String[] foreignKey,
			String referencedTable,
			String[] primaryKey,
			boolean referencesPrimaryKey) {
		final StringBuilder res = new StringBuilder( 30 );

		res.append( " add constraint " )
				.append( quote( constraintName ) )
				.append( " foreign key (" )
				.append( StringHelper.join( ", ", foreignKey ) )
				.append( ") references " )
				.append( referencedTable );

		if ( !referencesPrimaryKey ) {
			res.append( " (" )
					.append( StringHelper.join( ", ", primaryKey ) )
					.append( ')' );
		}

		return res.toString();
	}

	/**
	 * The syntax used to add a primary key constraint to a table.
	 *
	 * @param constraintName The name of the PK constraint.
	 * @return The "add PK" fragment
	 */
	public String getAddPrimaryKeyConstraintString(String constraintName) {
		return " add constraint " + constraintName + " primary key ";
	}

	/**
	 * Does the database/driver have bug in deleting rows that refer to other rows being deleted in the same query?
	 *
	 * @return {@code true} if the database/driver has this bug
	 */
	public boolean hasSelfReferentialForeignKeyBug() {
		return false;
	}

	/**
	 * The keyword used to specify a nullable column.
	 *
	 * @return String
	 */
	public String getNullColumnString() {
		return "";
	}

	/**
	 * Does this dialect/database support commenting on tables, columns, etc?
	 *
	 * @return {@code true} if commenting is supported
	 */
	public boolean supportsCommentOn() {
		return false;
	}

	/**
	 * Get the comment into a form supported for table definition.
	 *
	 * @param comment The comment to apply
	 *
	 * @return The comment fragment
	 */
	public String getTableComment(String comment) {
		return "";
	}

	/**
	 * Get the comment into a form supported for column definition.
	 *
	 * @param comment The comment to apply
	 *
	 * @return The comment fragment
	 */
	public String getColumnComment(String comment) {
		return "";
	}

	/**
	 * For dropping a table, can the phrase "if exists" be applied before the table name?
	 * <p/>
	 * NOTE : Only one or the other (or neither) of this and {@link #supportsIfExistsAfterTableName} should return true
	 *
	 * @return {@code true} if the "if exists" can be applied before the table name
	 */
	public boolean supportsIfExistsBeforeTableName() {
		return false;
	}

	/**
	 * For dropping a table, can the phrase "if exists" be applied after the table name?
	 * <p/>
	 * NOTE : Only one or the other (or neither) of this and {@link #supportsIfExistsBeforeTableName} should return true
	 *
	 * @return {@code true} if the "if exists" can be applied after the table name
	 */
	public boolean supportsIfExistsAfterTableName() {
		return false;
	}

	/**
	 * For dropping a constraint with an "alter table", can the phrase "if exists" be applied before the constraint name?
	 * <p/>
	 * NOTE : Only one or the other (or neither) of this and {@link #supportsIfExistsAfterConstraintName} should return true
	 *
	 * @return {@code true} if the "if exists" can be applied before the constraint name
	 */
	public boolean supportsIfExistsBeforeConstraintName() {
		return false;
	}

	/**
	 * For dropping a constraint with an "alter table", can the phrase "if exists" be applied after the constraint name?
	 * <p/>
	 * NOTE : Only one or the other (or neither) of this and {@link #supportsIfExistsBeforeConstraintName} should return true
	 *
	 * @return {@code true} if the "if exists" can be applied after the constraint name
	 */
	public boolean supportsIfExistsAfterConstraintName() {
		return false;
	}

	/**
	 * Generate a DROP TABLE statement
	 *
	 * @param tableName The name of the table to drop
	 *
	 * @return The DROP TABLE command
	 */
	public String getDropTableString(String tableName) {
		final StringBuilder buf = new StringBuilder( "drop table " );
		if ( supportsIfExistsBeforeTableName() ) {
			buf.append( "if exists " );
		}
		buf.append( tableName ).append( getCascadeConstraintsString() );
		if ( supportsIfExistsAfterTableName() ) {
			buf.append( " if exists" );
		}
		return buf.toString();
	}

	/**
	 * Does this dialect support column-level check constraints?
	 *
	 * @return True if column-level CHECK constraints are supported; false
	 * otherwise.
	 */
	public boolean supportsColumnCheck() {
		return true;
	}

	/**
	 * Does this dialect support table-level check constraints?
	 *
	 * @return True if table-level CHECK constraints are supported; false
	 * otherwise.
	 */
	public boolean supportsTableCheck() {
		return true;
	}

	/**
	 * Does this dialect support cascaded delete on foreign key definitions?
	 *
	 * @return {@code true} indicates that the dialect does support cascaded delete on foreign keys.
	 */
	public boolean supportsCascadeDelete() {
		return true;
	}

	/**
	 * Completely optional cascading drop clause
	 *
	 * @return String
	 */
	public String getCascadeConstraintsString() {
		return "";
	}

	/**
	 * Returns the separator to use for defining cross joins when translating HQL queries.
	 * <p/>
	 * Typically this will be either [<tt> cross join </tt>] or [<tt>, </tt>]
	 * <p/>
	 * Note that the spaces are important!
	 *
	 * @return The cross join separator
	 */
	public String getCrossJoinSeparator() {
		return " cross join ";
	}

	public ColumnAliasExtractor getColumnAliasExtractor() {
		return ColumnAliasExtractor.COLUMN_LABEL_EXTRACTOR;
	}


	// Informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Does this dialect support empty IN lists?
	 * <p/>
	 * For example, is [where XYZ in ()] a supported construct?
	 *
	 * @return True if empty in lists are supported; false otherwise.
	 * @since 3.2
	 */
	public boolean supportsEmptyInList() {
		return true;
	}

	/**
	 * Are string comparisons implicitly case insensitive.
	 * <p/>
	 * In other words, does [where 'XYZ' = 'xyz'] resolve to true?
	 *
	 * @return True if comparisons are case insensitive.
	 * @since 3.2
	 */
	public boolean areStringComparisonsCaseInsensitive() {
		return false;
	}

	/**
	 * Is this dialect known to support what ANSI-SQL terms "row value
	 * constructor" syntax; sometimes called tuple syntax.
	 * <p/>
	 * Basically, does it support syntax like
	 * "... where (FIRST_NAME, LAST_NAME) = ('Steve', 'Ebersole') ...".
	 *
	 * @return True if this SQL dialect is known to support "row value
	 * constructor" syntax; false otherwise.
	 * @since 3.2
	 */
	public boolean supportsRowValueConstructorSyntax() {
		// return false here, as most databases do not properly support this construct...
		return false;
	}

	/**
	 * If the dialect supports {@link #supportsRowValueConstructorSyntax() row values},
	 * does it offer such support in IN lists as well?
	 * <p/>
	 * For example, "... where (FIRST_NAME, LAST_NAME) IN ( (?, ?), (?, ?) ) ..."
	 *
	 * @return True if this SQL dialect is known to support "row value
	 * constructor" syntax in the IN list; false otherwise.
	 * @since 3.2
	 */
	public boolean supportsRowValueConstructorSyntaxInInList() {
		return false;
	}

	/**
	 * Should LOBs (both BLOB and CLOB) be bound using stream operations (i.e.
	 * {@link java.sql.PreparedStatement#setBinaryStream}).
	 *
	 * @return True if BLOBs and CLOBs should be bound using stream operations.
	 * @since 3.2
	 */
	public boolean useInputStreamToInsertBlob() {
		return true;
	}

	/**
	 * Does this dialect support parameters within the <tt>SELECT</tt> clause of
	 * <tt>INSERT ... SELECT ...</tt> statements?
	 *
	 * @return True if this is supported; false otherwise.
	 * @since 3.2
	 */
	public boolean supportsParametersInInsertSelect() {
		return true;
	}

	/**
	 * Does this dialect require that references to result variables
	 * (i.e, select expresssion aliases) in an ORDER BY clause be
	 * replaced by column positions (1-origin) as defined
	 * by the select clause?

	 * @return true if result variable references in the ORDER BY
	 *              clause should be replaced by column positions;
	 *         false otherwise.
	 */
	public boolean replaceResultVariableInOrderByClauseWithPosition() {
		return false;
	}

	/**
	 * Renders an ordering fragment
	 *
	 * @param expression The SQL order expression. In case of {@code @OrderBy} annotation user receives property placeholder
	 * (e.g. attribute name enclosed in '{' and '}' signs).
	 * @param collation Collation string in format {@code collate IDENTIFIER}, or {@code null}
	 * if expression has not been explicitly specified.
	 * @param order Order direction. Possible values: {@code asc}, {@code desc}, or {@code null}
	 * if expression has not been explicitly specified.
	 * @param nulls Nulls precedence. Default value: {@link NullPrecedence#NONE}.
	 * @return Renders single element of {@code ORDER BY} clause.
	 */
	public String renderOrderByElement(String expression, String collation, String order, NullPrecedence nulls) {
		final StringBuilder orderByElement = new StringBuilder( expression );
		if ( collation != null ) {
			orderByElement.append( " " ).append( collation );
		}
		if ( order != null ) {
			orderByElement.append( " " ).append( order );
		}
		if ( nulls != NullPrecedence.NONE ) {
			orderByElement.append( " nulls " ).append( nulls.name().toLowerCase(Locale.ROOT) );
		}
		return orderByElement.toString();
	}

	/**
	 * Does this dialect require that parameters appearing in the <tt>SELECT</tt> clause be wrapped in <tt>cast()</tt>
	 * calls to tell the db parser the expected type.
	 *
	 * @return True if select clause parameter must be cast()ed
	 * @since 3.2
	 */
	public boolean requiresCastingOfParametersInSelectClause() {
		return false;
	}

	/**
	 * Does this dialect support asking the result set its positioning
	 * information on forward only cursors.  Specifically, in the case of
	 * scrolling fetches, Hibernate needs to use
	 * {@link java.sql.ResultSet#isAfterLast} and
	 * {@link java.sql.ResultSet#isBeforeFirst}.  Certain drivers do not
	 * allow access to these methods for forward only cursors.
	 * <p/>
	 * NOTE : this is highly driver dependent!
	 *
	 * @return True if methods like {@link java.sql.ResultSet#isAfterLast} and
	 * {@link java.sql.ResultSet#isBeforeFirst} are supported for forward
	 * only cursors; false otherwise.
	 * @since 3.2
	 */
	public boolean supportsResultSetPositionQueryMethodsOnForwardOnlyCursor() {
		return true;
	}

	/**
	 * Does this dialect support definition of cascade delete constraints
	 * which can cause circular chains?
	 *
	 * @return True if circular cascade delete constraints are supported; false
	 * otherwise.
	 * @since 3.2
	 */
	public boolean supportsCircularCascadeDeleteConstraints() {
		return true;
	}

	/**
	 * Are subselects supported as the left-hand-side (LHS) of
	 * IN-predicates.
	 * <p/>
	 * In other words, is syntax like "... <subquery> IN (1, 2, 3) ..." supported?
	 *
	 * @return True if subselects can appear as the LHS of an in-predicate;
	 * false otherwise.
	 * @since 3.2
	 */
	public boolean  supportsSubselectAsInPredicateLHS() {
		return true;
	}

	/**
	 * Expected LOB usage pattern is such that I can perform an insert
	 * via prepared statement with a parameter binding for a LOB value
	 * without crazy casting to JDBC driver implementation-specific classes...
	 * <p/>
	 * Part of the trickiness here is the fact that this is largely
	 * driver dependent.  For example, Oracle (which is notoriously bad with
	 * LOB support in their drivers historically) actually does a pretty good
	 * job with LOB support as of the 10.2.x versions of their drivers...
	 *
	 * @return True if normal LOB usage patterns can be used with this driver;
	 * false if driver-specific hookiness needs to be applied.
	 * @since 3.2
	 */
	public boolean supportsExpectedLobUsagePattern() {
		return true;
	}

	/**
	 * Does the dialect support propagating changes to LOB
	 * values back to the database?  Talking about mutating the
	 * internal value of the locator as opposed to supplying a new
	 * locator instance...
	 * <p/>
	 * For BLOBs, the internal value might be changed by:
	 * {@link java.sql.Blob#setBinaryStream},
	 * {@link java.sql.Blob#setBytes(long, byte[])},
	 * {@link java.sql.Blob#setBytes(long, byte[], int, int)},
	 * or {@link java.sql.Blob#truncate(long)}.
	 * <p/>
	 * For CLOBs, the internal value might be changed by:
	 * {@link java.sql.Clob#setAsciiStream(long)},
	 * {@link java.sql.Clob#setCharacterStream(long)},
	 * {@link java.sql.Clob#setString(long, String)},
	 * {@link java.sql.Clob#setString(long, String, int, int)},
	 * or {@link java.sql.Clob#truncate(long)}.
	 * <p/>
	 * NOTE : I do not know the correct answer currently for
	 * databases which (1) are not part of the cruise control process
	 * or (2) do not {@link #supportsExpectedLobUsagePattern}.
	 *
	 * @return True if the changes are propagated back to the
	 * database; false otherwise.
	 * @since 3.2
	 */
	public boolean supportsLobValueChangePropogation() {
		// todo : pretty sure this is the same as the java.sql.DatabaseMetaData.locatorsUpdateCopy method added in JDBC 4, see HHH-6046
		return true;
	}

	/**
	 * Is it supported to materialize a LOB locator outside the transaction in
	 * which it was created?
	 * <p/>
	 * Again, part of the trickiness here is the fact that this is largely
	 * driver dependent.
	 * <p/>
	 * NOTE: all database I have tested which {@link #supportsExpectedLobUsagePattern()}
	 * also support the ability to materialize a LOB outside the owning transaction...
	 *
	 * @return True if unbounded materialization is supported; false otherwise.
	 * @since 3.2
	 */
	public boolean supportsUnboundedLobLocatorMaterialization() {
		return true;
	}

	/**
	 * Does this dialect support referencing the table being mutated in
	 * a subquery.  The "table being mutated" is the table referenced in
	 * an UPDATE or a DELETE query.  And so can that table then be
	 * referenced in a subquery of said UPDATE/DELETE query.
	 * <p/>
	 * For example, would the following two syntaxes be supported:<ul>
	 * <li>delete from TABLE_A where ID not in ( select ID from TABLE_A )</li>
	 * <li>update TABLE_A set NON_ID = 'something' where ID in ( select ID from TABLE_A)</li>
	 * </ul>
	 *
	 * @return True if this dialect allows references the mutating table from
	 * a subquery.
	 */
	public boolean supportsSubqueryOnMutatingTable() {
		return true;
	}

	/**
	 * Does the dialect support an exists statement in the select clause?
	 *
	 * @return True if exists checks are allowed in the select clause; false otherwise.
	 */
	public boolean supportsExistsInSelect() {
		return true;
	}

	/**
	 * For the underlying database, is READ_COMMITTED isolation implemented by
	 * forcing readers to wait for write locks to be released?
	 *
	 * @return True if writers block readers to achieve READ_COMMITTED; false otherwise.
	 */
	public boolean doesReadCommittedCauseWritersToBlockReaders() {
		return false;
	}

	/**
	 * For the underlying database, is REPEATABLE_READ isolation implemented by
	 * forcing writers to wait for read locks to be released?
	 *
	 * @return True if readers block writers to achieve REPEATABLE_READ; false otherwise.
	 */
	public boolean doesRepeatableReadCauseReadersToBlockWriters() {
		return false;
	}

	/**
	 * Does this dialect support using a JDBC bind parameter as an argument
	 * to a function or procedure call?
	 *
	 * @return Returns {@code true} if the database supports accepting bind params as args, {@code false} otherwise. The
	 * default is {@code true}.
	 */
	@SuppressWarnings( {"UnusedDeclaration"})
	public boolean supportsBindAsCallableArgument() {
		return true;
	}

	/**
	 * Does this dialect support `count(a,b)`?
	 *
	 * @return True if the database supports counting tuples; false otherwise.
	 */
	public boolean supportsTupleCounts() {
		return false;
	}

	/**
	 * Does this dialect support `count(distinct a,b)`?
	 *
	 * @return True if the database supports counting distinct tuples; false otherwise.
	 */
	public boolean supportsTupleDistinctCounts() {
		// oddly most database in fact seem to, so true is the default.
		return true;
	}

	/**
	 * If {@link #supportsTupleDistinctCounts()} is true, does the Dialect require the tuple to be wrapped with parens?
	 *
	 * @return boolean
	 */
	public boolean requiresParensForTupleDistinctCounts() {
		return false;
	}

	/**
	 * Return the limit that the underlying database places on the number elements in an {@code IN} predicate.
	 * If the database defines no such limits, simply return zero or less-than-zero.
	 *
	 * @return int The limit, or zero-or-less to indicate no limit.
	 */
	public int getInExpressionCountLimit() {
		return 0;
	}

	/**
	 * HHH-4635
	 * Oracle expects all Lob values to be last in inserts and updates.
	 *
	 * @return boolean True of Lob values should be last, false if it
	 * does not matter.
	 */
	public boolean forceLobAsLastValue() {
		return false;
	}

	/**
	 * Some dialects have trouble applying pessimistic locking depending upon what other query options are
	 * specified (paging, ordering, etc).  This method allows these dialects to request that locking be applied
	 * by subsequent selects.
	 *
	 * @return {@code true} indicates that the dialect requests that locking be applied by subsequent select;
	 * {@code false} (the default) indicates that locking should be applied to the main SQL statement..
	 */
	public boolean useFollowOnLocking() {
		return false;
	}

	/**
	 * Negate an expression
	 *
	 * @param expression The expression to negate
	 *
	 * @return The negated expression
	 */
	public String getNotExpression(String expression) {
		return "not " + expression;
	}

	/**
	 * Get the UniqueDelegate supported by this dialect
	 *
	 * @return The UniqueDelegate
	 */
	public UniqueDelegate getUniqueDelegate() {
		return uniqueDelegate;
	}

	/**
	 * Does this dialect support the <tt>UNIQUE</tt> column syntax?
	 *
	 * @return boolean
	 *
	 * @deprecated {@link #getUniqueDelegate()} should be overridden instead.
	 */
	@Deprecated
	public boolean supportsUnique() {
		return true;
	}

	/**
	 * Does this dialect support adding Unique constraints via create and alter table ?
	 *
	 * @return boolean
	 *
	 * @deprecated {@link #getUniqueDelegate()} should be overridden instead.
	 */
	@Deprecated
	public boolean supportsUniqueConstraintInCreateAlterTable() {
		return true;
	}

	/**
	 * The syntax used to add a unique constraint to a table.
	 *
	 * @param constraintName The name of the unique constraint.
	 * @return The "add unique" fragment
	 *
	 * @deprecated {@link #getUniqueDelegate()} should be overridden instead.
	 */
	@Deprecated
	public String getAddUniqueConstraintString(String constraintName) {
		return " add constraint " + constraintName + " unique ";
	}

	/**
	 * Is the combination of not-null and unique supported?
	 *
	 * @return deprecated
	 *
	 * @deprecated {@link #getUniqueDelegate()} should be overridden instead.
	 */
	@Deprecated
	public boolean supportsNotNullUnique() {
		return true;
	}

	/**
	 * Apply a hint to the query.  The entire query is provided, allowing the Dialect full control over the placement
	 * and syntax of the hint.  By default, ignore the hint and simply return the query.
	 *
	 * @param query The query to which to apply the hint.
	 * @param hints The  hints to apply
	 * @return The modified SQL
	 */
	public String getQueryHintString(String query, List<String> hints) {
		return query;
	}

	/**
	 * Certain dialects support a subset of ScrollModes.  Provide a default to be used by Criteria and Query.
	 *
	 * @return ScrollMode
	 */
	public ScrollMode defaultScrollMode() {
		return ScrollMode.SCROLL_INSENSITIVE;
	}

	/**
	 * Does this dialect support tuples in subqueries?  Ex:
	 * delete from Table1 where (col1, col2) in (select col1, col2 from Table2)
	 *
	 * @return boolean
	 */
	public boolean supportsTuplesInSubqueries() {
		return true;
	}

	public CallableStatementSupport getCallableStatementSupport() {
		// most databases do not support returning cursors (ref_cursor)...
		return StandardCallableStatementSupport.NO_REF_CURSOR_INSTANCE;
	}

	/**
	 * By default interpret this based on DatabaseMetaData.
	 *
	 * @return
	 */
	public NameQualifierSupport getNameQualifierSupport() {
		return null;
	}
}
