/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.util.ArrayList;

import org.hibernate.type.descriptor.java.GenericArrayTypeDescriptor;
import org.hibernate.type.descriptor.sql.ArrayTypeDescriptor;

/**
 * @author Jordan Gigov
 */
public class ArrayTypes<T>
		extends AbstractSingleColumnStandardBasicType<T[]> {

	// Taken directly from org.hibernate.type.StandardBasicTypes
	public static final ArrayTypes BOOLEAN = new ArrayTypes<>( BooleanType.INSTANCE );
	public static final ArrayTypes NUMERIC_BOOLEAN = new ArrayTypes<>( NumericBooleanType.INSTANCE );
	public static final ArrayTypes TRUE_FALSE = new ArrayTypes<>( TrueFalseType.INSTANCE );
	public static final ArrayTypes YES_NO = new ArrayTypes<>( YesNoType.INSTANCE );
	public static final ArrayTypes BYTE = new ArrayTypes<>( ByteType.INSTANCE );
	public static final ArrayTypes SHORT = new ArrayTypes<>( ShortType.INSTANCE );
	public static final ArrayTypes INTEGER = new ArrayTypes<>( IntegerType.INSTANCE );
	public static final ArrayTypes LONG = new ArrayTypes<>( LongType.INSTANCE );
	public static final ArrayTypes FLOAT = new ArrayTypes<>( FloatType.INSTANCE );
	public static final ArrayTypes DOUBLE = new ArrayTypes<>( DoubleType.INSTANCE );
	public static final ArrayTypes BIG_INTEGER = new ArrayTypes<>( BigIntegerType.INSTANCE );
	public static final ArrayTypes BIG_DECIMAL = new ArrayTypes<>( BigDecimalType.INSTANCE );
	public static final ArrayTypes CHARACTER = new ArrayTypes<>( CharacterType.INSTANCE );
	public static final ArrayTypes STRING = new ArrayTypes<>( StringType.INSTANCE );
	public static final ArrayTypes URL = new ArrayTypes<>( UrlType.INSTANCE );
	public static final ArrayTypes TIME = new ArrayTypes<>( TimeType.INSTANCE );
	public static final ArrayTypes DATE = new ArrayTypes<>( DateType.INSTANCE );
	public static final ArrayTypes TIMESTAMP = new ArrayTypes<>( TimestampType.INSTANCE );
	public static final ArrayTypes CALENDAR = new ArrayTypes<>( CalendarType.INSTANCE );
	public static final ArrayTypes CALENDAR_DATE = new ArrayTypes<>( CalendarDateType.INSTANCE );
	public static final ArrayTypes CLASS = new ArrayTypes<>( ClassType.INSTANCE );
	public static final ArrayTypes LOCALE = new ArrayTypes<>( LocaleType.INSTANCE );
	public static final ArrayTypes CURRENCY = new ArrayTypes<>( CurrencyType.INSTANCE );
	public static final ArrayTypes TIMEZONE = new ArrayTypes<>( TimeZoneType.INSTANCE );
	public static final ArrayTypes UUID_BINARY = new ArrayTypes<>( UUIDBinaryType.INSTANCE );
	public static final ArrayTypes UUID_CHAR = new ArrayTypes<>( UUIDCharType.INSTANCE );
	public static final ArrayTypes BINARY = new ArrayTypes<>( BinaryType.INSTANCE );
	public static final ArrayTypes WRAPPER_BINARY = new ArrayTypes<>( WrapperBinaryType.INSTANCE );
	public static final ArrayTypes IMAGE = new ArrayTypes<>( ImageType.INSTANCE );
	public static final ArrayTypes BLOB = new ArrayTypes<>( BlobType.INSTANCE );
	public static final ArrayTypes MATERIALIZED_BLOB = new ArrayTypes<>( MaterializedBlobType.INSTANCE );
	public static final ArrayTypes CHAR_ARRAY = new ArrayTypes<>( CharArrayType.INSTANCE );
	public static final ArrayTypes CHARACTER_ARRAY = new ArrayTypes<>( CharacterArrayType.INSTANCE );
	public static final ArrayTypes TEXT = new ArrayTypes<>( TextType.INSTANCE );
	public static final ArrayTypes NTEXT = new ArrayTypes<>( NTextType.INSTANCE );
	public static final ArrayTypes CLOB = new ArrayTypes<>( ClobType.INSTANCE );
	public static final ArrayTypes NCLOB = new ArrayTypes<>( NClobType.INSTANCE );
	public static final ArrayTypes MATERIALIZED_CLOB = new ArrayTypes<>( MaterializedClobType.INSTANCE );
	public static final ArrayTypes MATERIALIZED_NCLOB = new ArrayTypes<>( MaterializedNClobType.INSTANCE );
	public static final ArrayTypes SERIALIZABLE = new ArrayTypes<>( SerializableType.INSTANCE );

	// Java 8 time classes
	public static final ArrayTypes INSTANT = new ArrayTypes<>( InstantType.INSTANCE, java.sql.Timestamp.class );
	public static final ArrayTypes DURATION = new ArrayTypes<>( DurationType.INSTANCE, String.class );
	public static final ArrayTypes LOCAL_DATE_TIME = new ArrayTypes<>( LocalDateTimeType.INSTANCE, java.sql.Timestamp.class );
	public static final ArrayTypes LOCAL_DATE = new ArrayTypes<>( LocalDateType.INSTANCE, java.sql.Date.class );
	public static final ArrayTypes LOCAL_TIME = new ArrayTypes<>( LocalTimeType.INSTANCE, java.sql.Time.class );
	public static final ArrayTypes ZONED_DATE_TIME = new ArrayTypes<>( ZonedDateTimeType.INSTANCE, java.sql.Timestamp.class );
	public static final ArrayTypes OFFSET_DATE_TIME = new ArrayTypes<>( OffsetDateTimeType.INSTANCE, java.sql.Timestamp.class );
	public static final ArrayTypes OFFSET_TIME = new ArrayTypes<>( OffsetTimeType.INSTANCE, java.sql.Time.class );

	// Shouldn't this type be deprecated in SQL already?
	public static final ArrayTypes STRING_N_VARCHAR = new ArrayTypes<>( StringNVarcharType.INSTANCE );

	private final AbstractStandardBasicType<T> baseDescriptor;
	private final String name;
	private final String[] regKeys;

	public ArrayTypes(AbstractStandardBasicType<T> baseDescriptor) {
		this( baseDescriptor, null );
	}

	public ArrayTypes(AbstractStandardBasicType<T> baseDescriptor, Class unwrap) {
		super( ArrayTypeDescriptor.INSTANCE, new GenericArrayTypeDescriptor<>( baseDescriptor, unwrap ) );
		this.baseDescriptor = baseDescriptor;
		this.name = baseDescriptor.getName() + "[]";
		this.regKeys = buildTypeRegistrations( baseDescriptor.getRegistrationKeys(), ArrayTypes.class.isInstance( baseDescriptor ) );
	}

	/**
	 * Builds the array registration keys, based on the original type's keys.
	 *
	 * @param baseKeys Array of keys used by the base type.
	 * @return
	 */
	private String[] buildTypeRegistrations(String[] baseKeys, boolean noSQLrecurse) {
		ArrayList<String> keys = new ArrayList<>( baseKeys.length << 1 );
		for ( String bk : baseKeys ) {
			String className;
			boolean addSQL = true;
			try {
				Class c;
				switch ( bk ) {
					case "boolean":
						c = boolean.class;
						className = "Z";
						break;

					case "byte":
						c = byte.class;
						className = "B";
						break;

					case "char":
						c = char.class;
						className = "C";
						break;

					case "double":
						c = double.class;
						className = "D";
						break;

					case "float":
						c = float.class;
						className = "F";
						break;

					case "int":
						c = int.class;
						className = "I";
						break;

					case "long":
						c = long.class;
						className = "J";
						break;

					case "short":
						c = short.class;
						className = "S";
						break;

					default:
						// load to make sure it exists
						c = Class.forName( bk );
						className = c.getName();
						addSQL = false;
				}
				if ( c.isPrimitive() || c.isArray() ) {
					keys.add( "[" + className );
				}
				else {
					keys.add( "[L" + className + ";" );
				}
			}
			catch ( ClassNotFoundException ex ) {
			}
			if ( addSQL ) {
				// Not all type names given are Java classes, so assume the others are Database types
				if ( noSQLrecurse ) {
					// type is just "basetype ARRAY", never "basetype ARRAY ARRAY ARRAY"
					keys.add( bk );
				}
				else {
					// PostgreSQL type names
					keys.add( bk + "[]" );
					// standard SQL
					keys.add( bk + " ARRAY" );
					// also possible
					keys.add( bk + " array" );
				}
			}
		}
		return keys.toArray( new String[keys.size()] );
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String[] getRegistrationKeys() {
		return (String[]) regKeys.clone();
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

}
