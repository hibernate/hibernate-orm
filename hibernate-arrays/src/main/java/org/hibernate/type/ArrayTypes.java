package org.hibernate.type;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.java.GenericArrayTypeDescriptor;
import org.hibernate.type.BigIntegerType;
import org.hibernate.type.BooleanType;
import org.hibernate.type.CalendarType;
import org.hibernate.type.CurrencyType;
import org.hibernate.type.DateType;
import org.hibernate.type.DoubleType;
import org.hibernate.type.DurationType;
import org.hibernate.type.FloatType;
import org.hibernate.type.ImageType;
import org.hibernate.type.InstantType;
import org.hibernate.type.IntegerType;
import org.hibernate.type.LocalDateTimeType;
import org.hibernate.type.LocalDateType;
import org.hibernate.type.LocalTimeType;
import org.hibernate.type.LocaleType;
import org.hibernate.type.LongType;
import org.hibernate.type.NClobType;
import org.hibernate.type.NTextType;
import org.hibernate.type.NumericBooleanType;
import org.hibernate.type.OffsetDateTimeType;
import org.hibernate.type.OffsetTimeType;
import org.hibernate.type.SerializableType;
import org.hibernate.type.ShortType;
import org.hibernate.type.StringNVarcharType;
import org.hibernate.type.StringType;
import org.hibernate.type.TextType;
import org.hibernate.type.TimeType;
import org.hibernate.type.TimestampType;
import org.hibernate.type.TrueFalseType;
import org.hibernate.type.ZonedDateTimeType;
import org.hibernate.type.UrlType;
import org.hibernate.type.YesNoType;
import org.hibernate.type.descriptor.sql.ArrayTypeDescriptor;

public class ArrayTypes<T>
		extends AbstractSingleColumnStandardBasicType<T[]>
		implements LiteralType<T[]> {

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
	public static final ArrayTypes INSTANT = new ArrayTypes<>( InstantType.INSTANCE );
	public static final ArrayTypes DURATION = new ArrayTypes<>( DurationType.INSTANCE );
	public static final ArrayTypes LOCAL_DATE_TIME = new ArrayTypes<>( LocalDateTimeType.INSTANCE );
	public static final ArrayTypes LOCAL_DATE = new ArrayTypes<>( LocalDateType.INSTANCE );
	public static final ArrayTypes LOCAL_TIME = new ArrayTypes<>( LocalTimeType.INSTANCE );
	public static final ArrayTypes ZONED_DATE_TIME = new ArrayTypes<>( ZonedDateTimeType.INSTANCE );
	public static final ArrayTypes OFFSET_DATE_TIME = new ArrayTypes<>( OffsetDateTimeType.INSTANCE );
	public static final ArrayTypes OFFSET_TIME = new ArrayTypes<>( OffsetTimeType.INSTANCE );

	// Shouldn't this type be deprecated in SQL already?
	public static final ArrayTypes STRING_N_VARCHAR = new ArrayTypes<>( StringNVarcharType.INSTANCE );

	public ArrayTypes( AbstractStandardBasicType<T> baseDescriptor ) {
		super( ArrayTypeDescriptor.INSTANCE, new GenericArrayTypeDescriptor<>( baseDescriptor ) );
	}

	@Override
	public String getName() {
		return Integer[].class.getName();
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	@Override
	public String objectToSQLString( T[] value, Dialect dialect ) throws Exception {
		StringBuilder sb = new StringBuilder( "{" );
		for ( T i : value ) {
			if ( i == null ) {
				sb.append( "null" );
			} else {
				sb.append( dialect.quote( i.toString() ) );
			}
			sb.append( ',' );
		}
		sb.deleteCharAt( sb.length() - 1 );
		sb.append( '}' );
		return sb.toString();
	}

}
