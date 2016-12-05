/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi.descriptor.java;

/**
 *
 * @author Steve Ebersole
 */
public class JavaTypeDescriptorBaseline {
	interface BaselineTarget {
		void addBaselineDescriptor(JavaTypeDescriptorBasicImplementor descriptor);
		void addBaselineDescriptor(Class describedJavaType, JavaTypeDescriptorBasicImplementor descriptor);
	}

	public static void prime(BaselineTarget target) {
		target.addBaselineDescriptor( ByteTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( BooleanTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( CharacterTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( ShortTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( IntegerTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( LongTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( FloatTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( DoubleTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( BigDecimalTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( BigIntegerTypeDescriptor.INSTANCE );

		target.addBaselineDescriptor( StringTypeDescriptor.INSTANCE );

		target.addBaselineDescriptor( BlobTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( ClobTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( NClobTypeDescriptor.INSTANCE );

		target.addBaselineDescriptor( ByteArrayTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( CharacterArrayTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( PrimitiveByteArrayTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( PrimitiveCharacterArrayTypeDescriptor.INSTANCE );

		target.addBaselineDescriptor( DurationJavaDescriptor.INSTANCE );
		target.addBaselineDescriptor( InstantJavaDescriptor.INSTANCE );
		target.addBaselineDescriptor( LocalDateJavaDescriptor.INSTANCE );
		target.addBaselineDescriptor( LocalDateTimeJavaDescriptor.INSTANCE );
		target.addBaselineDescriptor( OffsetDateTimeJavaDescriptor.INSTANCE );
		target.addBaselineDescriptor( OffsetTimeJavaDescriptor.INSTANCE );
		target.addBaselineDescriptor( ZonedDateTimeJavaDescriptor.INSTANCE );

		target.addBaselineDescriptor( CalendarTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( DateTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( java.sql.Date.class, JdbcDateTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( java.sql.Time.class, JdbcTimeTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( java.sql.Timestamp.class, JdbcTimestampTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( TimeZoneTypeDescriptor.INSTANCE );

		target.addBaselineDescriptor( ClassTypeDescriptor.INSTANCE );

		target.addBaselineDescriptor( CurrencyTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( LocaleTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( UrlTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( UUIDTypeDescriptor.INSTANCE );

	}
}