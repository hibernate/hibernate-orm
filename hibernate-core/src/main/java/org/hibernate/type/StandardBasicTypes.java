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
package org.hibernate.type;

import java.util.HashSet;
import java.util.Set;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * Centralizes access to the standard set of basic {@link Type types}.
 * <p/>
 * Type mappings can be adjusted per {@link org.hibernate.SessionFactory}.  These adjusted mappings can be accessed
 * from the {@link org.hibernate.TypeHelper} instance obtained via {@link org.hibernate.SessionFactory#getTypeHelper()}
 *
 * @see BasicTypeRegistry
 * @see org.hibernate.TypeHelper
 * @see org.hibernate.SessionFactory#getTypeHelper()
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class StandardBasicTypes {

	private static final Set<SqlTypeDescriptor> sqlTypeDescriptors = new HashSet<SqlTypeDescriptor>();

	/**
	 * The standard Hibernate type for mapping {@link Boolean} to JDBC {@link java.sql.Types#BIT BIT}.
	 *
	 * @see BooleanType
	 */
	public static final BooleanType BOOLEAN = register( BooleanType.INSTANCE );

	/**
	 * The standard Hibernate type for mapping {@link Boolean} to JDBC {@link java.sql.Types#INTEGER INTEGER}.
	 *
	 * @see NumericBooleanType
	 */
	public static final NumericBooleanType NUMERIC_BOOLEAN = register( NumericBooleanType.INSTANCE );

	/**
	 * The standard Hibernate type for mapping {@link Boolean} to JDBC {@link java.sql.Types#CHAR CHAR(1)} (using 'T'/'F').
	 *
	 * @see TrueFalseType
	 */
	public static final TrueFalseType TRUE_FALSE = register( TrueFalseType.INSTANCE );

	/**
	 * The standard Hibernate type for mapping {@link Boolean} to JDBC {@link java.sql.Types#CHAR CHAR(1)} (using 'Y'/'N').
	 *
	 * @see YesNoType
	 */
	public static final YesNoType YES_NO = register( YesNoType.INSTANCE );

	/**
	 * The standard Hibernate type for mapping {@link Byte} to JDBC {@link java.sql.Types#TINYINT TINYINT}.
	 */
	public static final ByteType BYTE = register( ByteType.INSTANCE );

	/**
	 * The standard Hibernate type for mapping {@link Short} to JDBC {@link java.sql.Types#SMALLINT SMALLINT}.
	 *
	 * @see ShortType
	 */
	public static final ShortType SHORT = register( ShortType.INSTANCE );

	/**
	 * The standard Hibernate type for mapping {@link Integer} to JDBC {@link java.sql.Types#INTEGER INTEGER}.
	 *
	 * @see IntegerType
	 */
	public static final IntegerType INTEGER = register( IntegerType.INSTANCE );

	/**
	 * The standard Hibernate type for mapping {@link Long} to JDBC {@link java.sql.Types#BIGINT BIGINT}.
	 *
	 * @see LongType
	 */
	public static final LongType LONG = register( LongType.INSTANCE );

	/**
	 * The standard Hibernate type for mapping {@link Float} to JDBC {@link java.sql.Types#FLOAT FLOAT}.
	 *
	 * @see FloatType
	 */
	public static final FloatType FLOAT = register( FloatType.INSTANCE );

	/**
	 * The standard Hibernate type for mapping {@link Double} to JDBC {@link java.sql.Types#DOUBLE DOUBLE}.
	 *
	 * @see DoubleType
	 */
	public static final DoubleType DOUBLE = register( DoubleType.INSTANCE );

	/**
	 * The standard Hibernate type for mapping {@link java.math.BigInteger} to JDBC {@link java.sql.Types#NUMERIC NUMERIC}.
	 *
	 * @see BigIntegerType
	 */
	public static final BigIntegerType BIG_INTEGER = register( BigIntegerType.INSTANCE );

	/**
	 * The standard Hibernate type for mapping {@link java.math.BigDecimal} to JDBC {@link java.sql.Types#NUMERIC NUMERIC}.
	 *
	 * @see BigDecimalType
	 */
	public static final BigDecimalType BIG_DECIMAL = register( BigDecimalType.INSTANCE );

	/**
	 * The standard Hibernate type for mapping {@link Character} to JDBC {@link java.sql.Types#CHAR CHAR(1)}.
	 *
	 * @see CharacterType
	 */
	public static final CharacterType CHARACTER = register( CharacterType.INSTANCE );

	/**
	 * The standard Hibernate type for mapping {@link String} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 *
	 * @see StringType
	 */
	public static final StringType STRING = register( StringType.INSTANCE );

	/**
	 * The standard Hibernate type for mapping {@link java.net.URL} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 *
	 * @see UrlType
	 */
	public static final UrlType URL = register( UrlType.INSTANCE );

	/**
	 * The standard Hibernate type for mapping {@link java.util.Date} ({@link java.sql.Time}) to JDBC
	 * {@link java.sql.Types#TIME TIME}.
	 *
	 * @see TimeType
	 */
	public static final TimeType TIME = register( TimeType.INSTANCE );

	/**
	 * The standard Hibernate type for mapping {@link java.util.Date} ({@link java.sql.Date}) to JDBC
	 * {@link java.sql.Types#DATE DATE}.
	 *
	 * @see TimeType
	 */
	public static final DateType DATE = register( DateType.INSTANCE );

	/**
	 * The standard Hibernate type for mapping {@link java.util.Date} ({@link java.sql.Timestamp}) to JDBC
	 * {@link java.sql.Types#TIMESTAMP TIMESTAMP}.
	 *
	 * @see TimeType
	 */
	public static final TimestampType TIMESTAMP = register( TimestampType.INSTANCE );

	/**
	 * The standard Hibernate type for mapping {@link java.util.Calendar} to JDBC
	 * {@link java.sql.Types#TIMESTAMP TIMESTAMP}.
	 *
	 * @see CalendarType
	 */
	public static final CalendarType CALENDAR = register( CalendarType.INSTANCE );

	/**
	 * The standard Hibernate type for mapping {@link java.util.Calendar} to JDBC
	 * {@link java.sql.Types#DATE DATE}.
	 *
	 * @see CalendarDateType
	 */
	public static final CalendarDateType CALENDAR_DATE = register( CalendarDateType.INSTANCE );

	/**
	 * The standard Hibernate type for mapping {@link Class} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 *
	 * @see ClassType
	 */
	public static final ClassType CLASS = register( ClassType.INSTANCE );

	/**
	 * The standard Hibernate type for mapping {@link java.util.Locale} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 *
	 * @see LocaleType
	 */
	public static final LocaleType LOCALE = register( LocaleType.INSTANCE );

	/**
	 * The standard Hibernate type for mapping {@link java.util.Currency} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 *
	 * @see CurrencyType
	 */
	public static final CurrencyType CURRENCY = register( CurrencyType.INSTANCE );

	/**
	 * The standard Hibernate type for mapping {@link java.util.TimeZone} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 *
	 * @see TimeZoneType
	 */
	public static final TimeZoneType TIMEZONE = register( TimeZoneType.INSTANCE );

	/**
	 * The standard Hibernate type for mapping {@link java.util.UUID} to JDBC {@link java.sql.Types#BINARY BINARY}.
	 *
	 * @see UUIDBinaryType
	 */
	public static final UUIDBinaryType UUID_BINARY = register( UUIDBinaryType.INSTANCE );

	/**
	 * The standard Hibernate type for mapping {@link java.util.UUID} to JDBC {@link java.sql.Types#CHAR CHAR}.
	 *
	 * @see UUIDCharType
	 */
	public static final UUIDCharType UUID_CHAR = register( UUIDCharType.INSTANCE );

	/**
	 * The standard Hibernate type for mapping {@code byte[]} to JDBC {@link java.sql.Types#VARBINARY VARBINARY}.
	 *
	 * @see BinaryType
	 */
	public static final BinaryType BINARY = register( BinaryType.INSTANCE );

	/**
	 * The standard Hibernate type for mapping {@link Byte Byte[]} to JDBC {@link java.sql.Types#VARBINARY VARBINARY}.
	 *
	 * @see WrapperBinaryType
	 */
	public static final WrapperBinaryType WRAPPER_BINARY = register( WrapperBinaryType.INSTANCE );

	/**
	 * The standard Hibernate type for mapping {@code byte[]} to JDBC {@link java.sql.Types#LONGVARBINARY LONGVARBINARY}.
	 *
	 * @see ImageType
	 * @see #MATERIALIZED_BLOB
	 */
	public static final ImageType IMAGE = register( ImageType.INSTANCE );

	/**
	 * The standard Hibernate type for mapping {@link java.sql.Blob} to JDBC {@link java.sql.Types#BLOB BLOB}.
	 *
	 * @see BlobType
	 * @see #MATERIALIZED_BLOB
	 */
	public static final BlobType BLOB = register( BlobType.INSTANCE );

	/**
	 * The standard Hibernate type for mapping {@code byte[]} to JDBC {@link java.sql.Types#BLOB BLOB}.
	 *
	 * @see MaterializedBlobType
	 * @see #MATERIALIZED_BLOB
	 * @see #IMAGE
	 */
	public static final MaterializedBlobType MATERIALIZED_BLOB = register( MaterializedBlobType.INSTANCE );

	/**
	 * The standard Hibernate type for mapping {@code char[]} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 *
	 * @see CharArrayType
	 */
	public static final CharArrayType CHAR_ARRAY = register( CharArrayType.INSTANCE );

	/**
	 * The standard Hibernate type for mapping {@link Character Character[]} to JDBC
	 * {@link java.sql.Types#VARCHAR VARCHAR}.
	 *
	 * @see CharacterArrayType
	 */
	public static final CharacterArrayType CHARACTER_ARRAY = register( CharacterArrayType.INSTANCE );

	/**
	 * The standard Hibernate type for mapping {@link String} to JDBC {@link java.sql.Types#LONGVARCHAR LONGVARCHAR}.
	 * <p/>
	 * Similar to a {@link #MATERIALIZED_CLOB}
	 *
	 * @see TextType
	 */
	public static final TextType TEXT = register( TextType.INSTANCE );

	/**
	 * The standard Hibernate type for mapping {@link java.sql.Clob} to JDBC {@link java.sql.Types#CLOB CLOB}.
	 *
	 * @see ClobType
	 * @see #MATERIALIZED_CLOB
	 */
	public static final ClobType CLOB = register( ClobType.INSTANCE );

	/**
	 * The standard Hibernate type for mapping {@link String} to JDBC {@link java.sql.Types#CLOB CLOB}.
	 *
	 * @see MaterializedClobType
	 * @see #MATERIALIZED_CLOB
	 * @see #TEXT
	 */
	public static final MaterializedClobType MATERIALIZED_CLOB = register( MaterializedClobType.INSTANCE );

	/**
	 * The standard Hibernate type for mapping {@link java.io.Serializable} to JDBC {@link java.sql.Types#VARBINARY VARBINARY}.
	 * <p/>
	 * See especially the discussion wrt {@link ClassLoader} determination on {@link SerializableType}
	 *
	 * @see SerializableType
	 */
	public static final SerializableType SERIALIZABLE = register( SerializableType.INSTANCE );

	private static <T extends AbstractSingleColumnStandardBasicType> T register(T type) {
		sqlTypeDescriptors.add( type.getSqlTypeDescriptor() );
		return type;
	}

	public static final boolean isStandardBasicSqlTypeDescriptor(SqlTypeDescriptor sqlTypeDescriptor) {
		return sqlTypeDescriptors.contains( sqlTypeDescriptor );
	}
}
