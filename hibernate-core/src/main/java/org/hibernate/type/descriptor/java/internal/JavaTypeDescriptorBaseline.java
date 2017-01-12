/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.internal;

import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.descriptor.java.BigDecimalTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.BigIntegerTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.BlobTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.BooleanTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.ByteArrayTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.ByteTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.CalendarTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.CharacterArrayTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.CharacterTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.ClassTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.ClobTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.CurrencyTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.DateTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.DoubleTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.DurationJavaDescriptor;
import org.hibernate.type.spi.descriptor.java.FloatTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.InstantJavaDescriptor;
import org.hibernate.type.spi.descriptor.java.IntegerTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.JdbcDateTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.JdbcTimeTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.JdbcTimestampTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.LocalDateJavaDescriptor;
import org.hibernate.type.spi.descriptor.java.LocalDateTimeJavaDescriptor;
import org.hibernate.type.spi.descriptor.java.LocaleTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.LongTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.NClobTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.OffsetDateTimeJavaDescriptor;
import org.hibernate.type.spi.descriptor.java.OffsetTimeJavaDescriptor;
import org.hibernate.type.spi.descriptor.java.PrimitiveByteArrayTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.PrimitiveCharacterArrayTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.ShortTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.StringTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.TimeZoneTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.UUIDTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.UrlTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.ZonedDateTimeJavaDescriptor;

/**
 *
 * @author Steve Ebersole
 */
public class JavaTypeDescriptorBaseline {
	public interface BaselineTarget {
		void addBaselineDescriptor(BasicJavaDescriptor descriptor);
		void addBaselineDescriptor(Class describedJavaType, BasicJavaDescriptor descriptor);
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