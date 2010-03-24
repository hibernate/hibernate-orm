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
package org.hibernate.dialect;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.List;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.LockOptions;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.CastFunction;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.lock.*;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.exception.SQLExceptionConverter;
import org.hibernate.exception.SQLStateConverter;
import org.hibernate.exception.ViolatedConstraintNameExtracter;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.id.SequenceGenerator;
import org.hibernate.id.TableHiLoGenerator;
import org.hibernate.mapping.Column;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.sql.ANSICaseFragment;
import org.hibernate.sql.ANSIJoinFragment;
import org.hibernate.sql.CaseFragment;
import org.hibernate.sql.JoinFragment;
import org.hibernate.sql.ForUpdateFragment;
import org.hibernate.type.Type;
import org.hibernate.util.ReflectHelper;
import org.hibernate.util.StringHelper;

/**
 * Represents a dialect of SQL implemented by a particular RDBMS.
 * Subclasses implement Hibernate compatibility with different systems.<br>
 * <br>
 * Subclasses should provide a public default constructor that <tt>register()</tt>
 * a set of type mappings and default Hibernate properties.<br>
 * <br>
 * Subclasses should be immutable.
 *
 * @author Gavin King, David Channon
 */
public abstract class Dialect {

	private static final Logger log = LoggerFactory.getLogger( Dialect.class );

	public static final String DEFAULT_BATCH_SIZE = "15";
	public static final String NO_BATCH = "0";

	/**
	 * Characters used for quoting SQL identifiers
	 */
	public static final String QUOTE = "`\"[";
	public static final String CLOSED_QUOTE = "`\"]";


	// build the map of standard ANSI SQL aggregation functions ~~~~~~~~~~~~~~~

	private static final Map STANDARD_AGGREGATE_FUNCTIONS = new HashMap();

	static {
		STANDARD_AGGREGATE_FUNCTIONS.put(
				"count",
				new StandardSQLFunction("count") {
					public Type getReturnType(Type columnType, Mapping mapping) {
						return Hibernate.LONG;
					}
					public String render(List args, SessionFactoryImplementor factory) {
						if ( args.size() > 1 ) {
							if ( "distinct".equalsIgnoreCase( args.get( 0 ).toString() ) ) {
								return renderCountDistinct( args );
							}
						}
						return super.render( args, factory );
					}
					private String renderCountDistinct(List args) {
						StringBuffer buffer = new StringBuffer();
						buffer.append( "count(distinct " );
						String sep = "";
						Iterator itr = args.iterator();
						itr.next(); // intentionally skip first
						while ( itr.hasNext() ) {
							buffer.append( sep )
									.append( itr.next() );
							sep = ", ";
						}
						return buffer.append( ")" ).toString();
					}
				}
		);

		STANDARD_AGGREGATE_FUNCTIONS.put(
				"avg",
				new StandardSQLFunction("avg") {
					public Type getReturnType(Type columnType, Mapping mapping) throws QueryException {
						int[] sqlTypes;
						try {
							sqlTypes = columnType.sqlTypes( mapping );
						}
						catch ( MappingException me ) {
							throw new QueryException( me );
						}
						if ( sqlTypes.length != 1 ) {
							throw new QueryException( "multi-column type in avg()" );
						}
						return Hibernate.DOUBLE;
					}
				}
		);

		STANDARD_AGGREGATE_FUNCTIONS.put( "max", new StandardSQLFunction("max") );
		STANDARD_AGGREGATE_FUNCTIONS.put( "min", new StandardSQLFunction("min") );

		STANDARD_AGGREGATE_FUNCTIONS.put(
				"sum",
				new StandardSQLFunction("sum") {
					public Type getReturnType(Type columnType, Mapping mapping) {
						//pre H3.2 behavior: super.getReturnType(ct, m);
						int[] sqlTypes;
						try {
							sqlTypes = columnType.sqlTypes( mapping );
						}
						catch ( MappingException me ) {
							throw new QueryException( me );
						}
						if ( sqlTypes.length != 1 ) {
							throw new QueryException( "multi-column type in sum()" );
						}
						int sqlType = sqlTypes[0];

						// First allow the actual type to control the return value; the underlying sqltype could
						// actually be different
						if ( columnType == Hibernate.BIG_INTEGER ) {
							return Hibernate.BIG_INTEGER;
						}
						else if ( columnType == Hibernate.BIG_DECIMAL ) {
							return Hibernate.BIG_DECIMAL;
						}
						else if ( columnType == Hibernate.LONG
								|| columnType == Hibernate.SHORT
								|| columnType == Hibernate.INTEGER ) {
							return Hibernate.LONG;
						}
						else if ( columnType == Hibernate.FLOAT || columnType == Hibernate.DOUBLE)  {
							return Hibernate.DOUBLE;
						}

						// finally use the sqltype if == on Hibernate types did not find a match.
						if ( sqlType == Types.NUMERIC ) {
							return columnType; //because numeric can be anything
						}
						else if ( sqlType == Types.FLOAT
								|| sqlType == Types.DOUBLE
								|| sqlType == Types.DECIMAL
								|| sqlType == Types.REAL) {
							return Hibernate.DOUBLE;
						}
						else if ( sqlType == Types.BIGINT
								|| sqlType == Types.INTEGER
								|| sqlType == Types.SMALLINT
								|| sqlType == Types.TINYINT ) {
							return Hibernate.LONG;
						}
						else {
							return columnType;
						}
					}
				}
		);
	}

	private final TypeNames typeNames = new TypeNames();
	private final TypeNames hibernateTypeNames = new TypeNames();

	private final Properties properties = new Properties();
	private final Map sqlFunctions = new HashMap();
	private final Set sqlKeywords = new HashSet();


	// constructors and factory methods ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	protected Dialect() {
		log.info( "Using dialect: " + this );
		sqlFunctions.putAll( STANDARD_AGGREGATE_FUNCTIONS );

		// standard sql92 functions (can be overridden by subclasses)
		registerFunction( "substring", new SQLFunctionTemplate( Hibernate.STRING, "substring(?1, ?2, ?3)" ) );
		registerFunction( "locate", new SQLFunctionTemplate( Hibernate.INTEGER, "locate(?1, ?2, ?3)" ) );
		registerFunction( "trim", new SQLFunctionTemplate( Hibernate.STRING, "trim(?1 ?2 ?3 ?4)" ) );
		registerFunction( "length", new StandardSQLFunction( "length", Hibernate.INTEGER ) );
		registerFunction( "bit_length", new StandardSQLFunction( "bit_length", Hibernate.INTEGER ) );
		registerFunction( "coalesce", new StandardSQLFunction( "coalesce" ) );
		registerFunction( "nullif", new StandardSQLFunction( "nullif" ) );
		registerFunction( "abs", new StandardSQLFunction( "abs" ) );
		registerFunction( "mod", new StandardSQLFunction( "mod", Hibernate.INTEGER) );
		registerFunction( "sqrt", new StandardSQLFunction( "sqrt", Hibernate.DOUBLE) );
		registerFunction( "upper", new StandardSQLFunction("upper") );
		registerFunction( "lower", new StandardSQLFunction("lower") );
		registerFunction( "cast", new CastFunction() );
		registerFunction( "extract", new SQLFunctionTemplate(Hibernate.INTEGER, "extract(?1 ?2 ?3)") );

		//map second/minute/hour/day/month/year to ANSI extract(), override on subclasses
		registerFunction( "second", new SQLFunctionTemplate(Hibernate.INTEGER, "extract(second from ?1)") );
		registerFunction( "minute", new SQLFunctionTemplate(Hibernate.INTEGER, "extract(minute from ?1)") );
		registerFunction( "hour", new SQLFunctionTemplate(Hibernate.INTEGER, "extract(hour from ?1)") );
		registerFunction( "day", new SQLFunctionTemplate(Hibernate.INTEGER, "extract(day from ?1)") );
		registerFunction( "month", new SQLFunctionTemplate(Hibernate.INTEGER, "extract(month from ?1)") );
		registerFunction( "year", new SQLFunctionTemplate(Hibernate.INTEGER, "extract(year from ?1)") );

		registerFunction( "str", new SQLFunctionTemplate(Hibernate.STRING, "cast(?1 as char)") );

        // register hibernate types for default use in scalar sqlquery type auto detection
		registerHibernateType( Types.BIGINT, Hibernate.BIG_INTEGER.getName() );
		registerHibernateType( Types.BINARY, Hibernate.BINARY.getName() );
		registerHibernateType( Types.BIT, Hibernate.BOOLEAN.getName() );
		registerHibernateType( Types.CHAR, Hibernate.CHARACTER.getName() );
		registerHibernateType( Types.DATE, Hibernate.DATE.getName() );
		registerHibernateType( Types.DOUBLE, Hibernate.DOUBLE.getName() );
		registerHibernateType( Types.FLOAT, Hibernate.FLOAT.getName() );
		registerHibernateType( Types.INTEGER, Hibernate.INTEGER.getName() );
		registerHibernateType( Types.SMALLINT, Hibernate.SHORT.getName() );
		registerHibernateType( Types.TINYINT, Hibernate.BYTE.getName() );
		registerHibernateType( Types.TIME, Hibernate.TIME.getName() );
		registerHibernateType( Types.TIMESTAMP, Hibernate.TIMESTAMP.getName() );
		registerHibernateType( Types.VARCHAR, Hibernate.STRING.getName() );
		registerHibernateType( Types.VARBINARY, Hibernate.BINARY.getName() );
		registerHibernateType( Types.LONGVARCHAR, Hibernate.TEXT.getName() );
		registerHibernateType( Types.LONGVARBINARY, Hibernate.IMAGE.getName() );
		registerHibernateType( Types.NUMERIC, Hibernate.BIG_DECIMAL.getName() );
		registerHibernateType( Types.DECIMAL, Hibernate.BIG_DECIMAL.getName() );
		registerHibernateType( Types.BLOB, Hibernate.BLOB.getName() );
		registerHibernateType( Types.CLOB, Hibernate.CLOB.getName() );
		registerHibernateType( Types.REAL, Hibernate.FLOAT.getName() );
	}

	/**
	 * Get an instance of the dialect specified by the current <tt>System</tt> properties.
	 *
	 * @return The specified Dialect
	 * @throws HibernateException If no dialect was specified, or if it could not be instantiated.
	 */
	public static Dialect getDialect() throws HibernateException {
		String dialectName = Environment.getProperties().getProperty( Environment.DIALECT );
		return instantiateDialect( dialectName );
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
		String dialectName = props.getProperty( Environment.DIALECT );
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
			return ( Dialect ) ReflectHelper.classForName( dialectName ).newInstance();
		}
		catch ( ClassNotFoundException cnfe ) {
			throw new HibernateException( "Dialect class not found: " + dialectName );
		}
		catch ( Exception e ) {
			throw new HibernateException( "Could not instantiate dialect class", e );
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

	public String toString() {
		return getClass().getName();
	}


	// database type mapping support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Get the name of the database type associated with the given
	 * {@link java.sql.Types} typecode.
	 *
	 * @param code The {@link java.sql.Types} typecode
	 * @return the database type name
	 * @throws HibernateException If no mapping was specified for that type.
	 */
	public String getTypeName(int code) throws HibernateException {
		String result = typeNames.get( code );
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
	public String getTypeName(int code, int length, int precision, int scale) throws HibernateException {
		String result = typeNames.get( code, length, precision, scale );
		if ( result == null ) {
			throw new HibernateException(
					"No type mapping for java.sql.Types code: " +
					code +
					", length: " +
					length
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
	 * Subclasses register a type name for the given type code and maximum
	 * column length. <tt>$l</tt> in the type name with be replaced by the
	 * column length (if appropriate).
	 *
	 * @param code The {@link java.sql.Types} typecode
	 * @param capacity The maximum length of database type
	 * @param name The database type name
	 */
	protected void registerColumnType(int code, int capacity, String name) {
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


	// hibernate type mapping support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Get the name of the Hibernate {@link org.hibernate.type.Type} associated with the given
	 * {@link java.sql.Types} typecode.
	 *
	 * @param code The {@link java.sql.Types} typecode
	 * @return The Hibernate {@link org.hibernate.type.Type} name.
	 * @throws HibernateException If no mapping was specified for that type.
	 */
	public String getHibernateTypeName(int code) throws HibernateException {
		String result = hibernateTypeNames.get( code );
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
		String result = hibernateTypeNames.get( code, length, precision, scale );
		if ( result == null ) {
			throw new HibernateException(
					"No Hibernate type mapping for java.sql.Types code: " +
					code +
					", length: " +
					length
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
	protected void registerHibernateType(int code, int capacity, String name) {
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
		sqlFunctions.put( name, function );
	}

	/**
	 * Retrieves a map of the dialect's registered functions
	 * (functionName => {@link org.hibernate.dialect.function.SQLFunction}).
	 *
	 * @return The map of registered functions.
	 */
	public final Map getFunctions() {
		return sqlFunctions;
	}


	// keyword support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	protected void registerKeyword(String word) {
		sqlKeywords.add(word);
	}

	public Set getKeywords() {
		return sqlKeywords;
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
			return TableHiLoGenerator.class;
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
		throw new MappingException( "Dialect does not support identity key generation" );
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
		throw new MappingException( "Dialect does not support identity key generation" );
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
		throw new MappingException( "Dialect does not support sequences" );
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
		throw new MappingException( "Dialect does not support sequences" );
	}

	/**
	 * The multiline script used to create a sequence.
	 *
	 * @param sequenceName The name of the sequence
	 * @return The sequence creation commands
	 * @throws MappingException If sequences are not supported.
	 * @deprecated Use {@link #getCreateSequenceString(String, int, int)} instead
	 */
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
		throw new MappingException( "Dialect does not support sequences" );
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
		throw new MappingException( "Dialect does not support pooled sequences" );
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
		throw new MappingException( "Dialect does not support sequences" );
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


	// GUID support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Get the command used to select a GUID from the underlying database.
	 * <p/>
	 * Optional operation.
	 *
	 * @return The appropriate command.
	 */
	public String getSelectGUIDString() {
		throw new UnsupportedOperationException( "dialect does not support GUIDs" );
	}


	// limit/offset support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Does this dialect support some form of limiting query results
	 * via a SQL clause?
	 *
	 * @return True if this dialect supports some form of LIMIT.
	 */
	public boolean supportsLimit() {
		return false;
	}

	/**
	 * Does this dialect's LIMIT support (if any) additionally
	 * support specifying an offset?
	 *
	 * @return True if the dialect supports an offset within the limit support.
	 */
	public boolean supportsLimitOffset() {
		return supportsLimit();
	}

	/**
	 * Does this dialect support bind variables (i.e., prepared statement
	 * parameters) for its limit/offset?
	 *
	 * @return True if bind variables can be used; false otherwise.
	 */
	public boolean supportsVariableLimit() {
		return supportsLimit();
	}

	/**
	 * ANSI SQL defines the LIMIT clause to be in the form LIMIT offset, limit.
	 * Does this dialect require us to bind the parameters in reverse order?
	 *
	 * @return true if the correct order is limit, offset
	 */
	public boolean bindLimitParametersInReverseOrder() {
		return false;
	}

	/**
	 * Does the <tt>LIMIT</tt> clause come at the start of the
	 * <tt>SELECT</tt> statement, rather than at the end?
	 *
	 * @return true if limit parameters should come before other parameters
	 */
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
	 */
	public boolean useMaxForLimit() {
		return false;
	}

	/**
	 * Generally, if there is no limit applied to a Hibernate query we do not apply any limits
	 * to the SQL query.  This option forces that the limit be written to the SQL query.
	 *
	 * @return True to force limit into SQL query even if none specified in Hibernate query; false otherwise.
	 */
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
	 */
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
	 */
	protected String getLimitString(String query, boolean hasOffset) {
		throw new UnsupportedOperationException( "paged queries not supported" );
	}

	/**
	 * Hibernate APIs explicitly state that setFirstResult() should be a zero-based offset. Here we allow the
	 * Dialect a chance to convert that value based on what the underlying db or driver will expect.
	 * <p/>
	 * NOTE: what gets passed into {@link #getLimitString(String,int,int)} is the zero-based offset.  Dialects which
	 * do not {@link #supportsVariableLimit} should take care to perform any needed {@link #convertToFirstRowValue}
	 * calls prior to injecting the limit values into the SQL string.
	 *
	 * @param zeroBasedFirstResult The user-supplied, zero-based first row offset.
	 *
	 * @return The corresponding db/dialect specific offset.
	 *
	 * @see org.hibernate.Query#setFirstResult
	 * @see org.hibernate.Criteria#setFirstResult
	 */
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
		if ( lockMode==LockMode.PESSIMISTIC_FORCE_INCREMENT) {
			return new PessimisticForceIncrementLockingStrategy( lockable, lockMode);
		}
		else if ( lockMode==LockMode.PESSIMISTIC_WRITE) {
			return new PessimisticWriteSelectLockingStrategy( lockable, lockMode);
		}
		else if ( lockMode==LockMode.PESSIMISTIC_READ) {
			return new PessimisticReadSelectLockingStrategy( lockable, lockMode);
		}
		else if ( lockMode==LockMode.OPTIMISTIC) {
			return new OptimisticLockingStrategy( lockable, lockMode);
		}
		else if ( lockMode==LockMode.OPTIMISTIC_FORCE_INCREMENT) {
			return new OptimisticForceIncrementLockingStrategy( lockable, lockMode);
		}
		return new SelectLockingStrategy( lockable, lockMode );
	}

	/**
	 * Given LockOptions (lockMode, timeout), determine the appropriate for update fragment to use.
	 *
	 * @param lockOptions contains the lock mode to apply.
	 * @return The appropriate for update fragment.
	 */
	public String getForUpdateString(LockOptions lockOptions) {
		LockMode lockMode = lockOptions.getLockMode();
		if ( lockMode==LockMode.UPGRADE) {
			return getForUpdateString();
		}
		else if( lockMode==LockMode.PESSIMISTIC_READ ) {
			return getReadLockString(lockOptions.getTimeOut());
		}
		else if( lockMode==LockMode.PESSIMISTIC_WRITE ) {
			return getWriteLockString(lockOptions.getTimeOut());
		}
		else if ( lockMode==LockMode.UPGRADE_NOWAIT ) {
			return getForUpdateNowaitString();
		}
		else if ( lockMode==LockMode.FORCE || lockMode==LockMode.PESSIMISTIC_FORCE_INCREMENT) {
			return getForUpdateNowaitString();
		}
		else {
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
		if ( lockMode==LockMode.UPGRADE ) {
			return getForUpdateString();
		}
		else if( lockMode==LockMode.PESSIMISTIC_READ ) {
			return getReadLockString(LockOptions.WAIT_FOREVER);
		}
		else if( lockMode==LockMode.PESSIMISTIC_WRITE ) {
			return getWriteLockString(LockOptions.WAIT_FOREVER);
		}
		else if ( lockMode==LockMode.UPGRADE_NOWAIT ) {
			return getForUpdateNowaitString();
		}
		else if ( lockMode==LockMode.FORCE || lockMode==LockMode.PESSIMISTIC_FORCE_INCREMENT) {
			return getForUpdateNowaitString();
		}
		else {
			return "";
		}
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
	 * @param lockOptions 
	 * @return The appropriate <tt>FOR UPDATE OF column_list</tt> clause string.
	 */
	public String getForUpdateString(String aliases, LockOptions lockOptions) {
		// by default we simply return the getForUpdateString() result since
		// the default is to say no support for "FOR UPDATE OF ..."
		return getForUpdateString(lockOptions);
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
	 * Get the <tt>FOR UPDATE OF column_list NOWAIT</tt> fragment appropriate
	 * for this dialect given the aliases of the columns to be write locked.
	 *
	 * @param aliases The columns to be write locked.
	 * @return The appropriate <tt>FOR UPDATE colunm_list NOWAIT</tt> clause string.
	 */
	public String getForUpdateNowaitString(String aliases) {
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
	 */
	public String appendLockHint(LockMode mode, String tableName) {
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
	public String applyLocksToSql(String sql, LockOptions aliasedLockOptions, Map keyColumnNames) {
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


	// temporary table support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Does this dialect support temporary tables?
	 *
	 * @return True if temp tables are supported; false otherwise.
	 */
	public boolean supportsTemporaryTables() {
		return false;
	}

	/**
	 * Generate a temporary table name given the bas table.
	 *
	 * @param baseTableName The table name from which to base the temp table name.
	 * @return The generated temp table name.
	 */
	public String generateTemporaryTableName(String baseTableName) {
		return "HT_" + baseTableName;
	}

	/**
	 * Command used to create a temporary table.
	 *
	 * @return The command used to create a temporary table.
	 */
	public String getCreateTemporaryTableString() {
		return "create table";
	}

	/**
	 * Get any fragments needing to be postfixed to the command for
	 * temporary table creation.
	 *
	 * @return Any required postfix.
	 */
	public String getCreateTemporaryTablePostfix() {
		return "";
	}

	/**
	 * Command used to drop a temporary table.
	 *
	 * @return The command used to drop a temporary table.
	 */
	public String getDropTemporaryTableString() {
		return "drop table";
	}

	/**
	 * Does the dialect require that temporary table DDL statements occur in
	 * isolation from other statements?  This would be the case if the creation
	 * would cause any current transaction to get committed implicitly.
	 * <p/>
	 * JDBC defines a standard way to query for this information via the
	 * {@link java.sql.DatabaseMetaData#dataDefinitionCausesTransactionCommit()}
	 * method.  However, that does not distinguish between temporary table
	 * DDL and other forms of DDL; MySQL, for example, reports DDL causing a
	 * transaction commit via its driver, even though that is not the case for
	 * temporary table DDL.
	 * <p/>
	 * Possible return values and their meanings:<ul>
	 * <li>{@link Boolean#TRUE} - Unequivocally, perform the temporary table DDL
	 * in isolation.</li>
	 * <li>{@link Boolean#FALSE} - Unequivocally, do <b>not</b> perform the
	 * temporary table DDL in isolation.</li>
	 * <li><i>null</i> - defer to the JDBC driver response in regards to
	 * {@link java.sql.DatabaseMetaData#dataDefinitionCausesTransactionCommit()}</li>
	 * </ul>
	 * 
	 * @return see the result matrix above.
	 */
	public Boolean performTemporaryTableDDLInIsolation() {
		return null;
	}

	/**
	 * Do we need to drop the temporary table after use?
	 *
	 * @return True if the table should be dropped.
	 */
	public boolean dropTemporaryTableAfterUse() {
		return true;
	}


	// callable statement support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Registers an OUT parameter which will be returning a
	 * {@link java.sql.ResultSet}.  How this is accomplished varies greatly
	 * from DB to DB, hence its inclusion (along with {@link #getResultSet}) here.
	 *
	 * @param statement The callable statement.
	 * @param position The bind position at which to register the OUT param.
	 * @return The number of (contiguous) bind positions used.
	 * @throws SQLException Indicates problems registering the OUT param.
	 */
	public int registerResultSetOutParameter(CallableStatement statement, int position) throws SQLException {
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
				getClass().getName() +
				" does not support resultsets via stored procedures"
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
	 * converting SQLExceptions into Hibernate's JDBCException hierarchy.  The default
	 * Dialect implementation simply returns a converter based on X/Open SQLState codes.
	 * <p/>
	 * It is strongly recommended that specific Dialect implementations override this
	 * method, since interpretation of a SQL error is much more accurate when based on
	 * the ErrorCode rather than the SQLState.  Unfortunately, the ErrorCode is a vendor-
	 * specific approach.
	 *
	 * @return The Dialect's preferred SQLExceptionConverter.
	 */
	public SQLExceptionConverter buildSQLExceptionConverter() {
		// The default SQLExceptionConverter for all dialects is based on SQLState
		// since SQLErrorCode is extremely vendor-specific.  Specific Dialects
		// may override to return whatever is most appropriate for that vendor.
		return new SQLStateConverter( getViolatedConstraintNameExtracter() );
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
	 * Does this dialect support the <tt>UNIQUE</tt> column syntax?
	 *
	 * @return boolean
	 */
	public boolean supportsUnique() {
		return true;
	}

    /**
     * Does this dialect support adding Unique constraints via create and alter table ?
     * @return boolean
     */
	public boolean supportsUniqueConstraintInCreateAlterTable() {
	    return true;
	}

	/**
	 * The syntax used to add a column to a table (optional).
	 *
	 * @return The "add column" fragment.
	 */
	public String getAddColumnString() {
		throw new UnsupportedOperationException( "No add column syntax supported by Dialect" );
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
		StringBuffer res = new StringBuffer( 30 );

		res.append( " add constraint " )
				.append( constraintName )
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

	public boolean supportsCommentOn() {
		return false;
	}

	public String getTableComment(String comment) {
		return "";
	}

	public String getColumnComment(String comment) {
		return "";
	}

	public boolean supportsIfExistsBeforeTableName() {
		return false;
	}

	public boolean supportsIfExistsAfterTableName() {
		return false;
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

	public boolean supportsCascadeDelete() {
		return true;
	}

	public boolean supportsNotNullUnique() {
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
	 * Get the separator to use for defining cross joins when translating HQL queries.
	 * <p/>
	 * Typically this will be either [<tt> cross join </tt>] or [<tt>, </tt>]
	 * <p/>
	 * Note that the spaces are important!
	 *
	 * @return
	 */
	public String getCrossJoinSeparator() {
		return " cross join ";
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
	 * Does the dialect support propogating changes to LOB
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
	 * @return True if the changes are propogated back to the
	 * database; false otherwise.
	 * @since 3.2
	 */
	public boolean supportsLobValueChangePropogation() {
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
	 * @return True if the database supports accepting bind params as args; false otherwise.
	 */
	public boolean supportsBindAsCallableArgument() {
		return true;
	}
}
