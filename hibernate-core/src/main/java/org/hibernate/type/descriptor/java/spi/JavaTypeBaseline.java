/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java.spi;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

import org.hibernate.collection.internal.StandardArraySemantics;
import org.hibernate.collection.internal.StandardBagSemantics;
import org.hibernate.collection.internal.StandardListSemantics;
import org.hibernate.collection.internal.StandardMapSemantics;
import org.hibernate.collection.internal.StandardSetSemantics;
import org.hibernate.collection.internal.StandardSortedMapSemantics;
import org.hibernate.collection.internal.StandardSortedSetSemantics;
import org.hibernate.type.descriptor.java.BigDecimalJavaType;
import org.hibernate.type.descriptor.java.BigIntegerJavaType;
import org.hibernate.type.descriptor.java.BlobJavaType;
import org.hibernate.type.descriptor.java.BooleanJavaType;
import org.hibernate.type.descriptor.java.BooleanPrimitiveArrayJavaType;
import org.hibernate.type.descriptor.java.ByteJavaType;
import org.hibernate.type.descriptor.java.CalendarJavaType;
import org.hibernate.type.descriptor.java.CharacterJavaType;
import org.hibernate.type.descriptor.java.ClassJavaType;
import org.hibernate.type.descriptor.java.ClobJavaType;
import org.hibernate.type.descriptor.java.CurrencyJavaType;
import org.hibernate.type.descriptor.java.DateJavaType;
import org.hibernate.type.descriptor.java.DoubleJavaType;
import org.hibernate.type.descriptor.java.DoublePrimitiveArrayJavaType;
import org.hibernate.type.descriptor.java.DurationJavaType;
import org.hibernate.type.descriptor.java.FloatJavaType;
import org.hibernate.type.descriptor.java.FloatPrimitiveArrayJavaType;
import org.hibernate.type.descriptor.java.InetAddressJavaType;
import org.hibernate.type.descriptor.java.InstantJavaType;
import org.hibernate.type.descriptor.java.IntegerPrimitiveArrayJavaType;
import org.hibernate.type.descriptor.java.IntegerJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.LongPrimitiveArrayJavaType;
import org.hibernate.type.descriptor.java.ObjectJavaType;
import org.hibernate.type.descriptor.java.JdbcDateJavaType;
import org.hibernate.type.descriptor.java.JdbcTimeJavaType;
import org.hibernate.type.descriptor.java.JdbcTimestampJavaType;
import org.hibernate.type.descriptor.java.LocalDateJavaType;
import org.hibernate.type.descriptor.java.LocalDateTimeJavaType;
import org.hibernate.type.descriptor.java.LocalTimeJavaType;
import org.hibernate.type.descriptor.java.LocaleJavaType;
import org.hibernate.type.descriptor.java.LongJavaType;
import org.hibernate.type.descriptor.java.NClobJavaType;
import org.hibernate.type.descriptor.java.OffsetDateTimeJavaType;
import org.hibernate.type.descriptor.java.OffsetTimeJavaType;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;
import org.hibernate.type.descriptor.java.PrimitiveCharacterArrayJavaType;
import org.hibernate.type.descriptor.java.ShortJavaType;
import org.hibernate.type.descriptor.java.ShortPrimitiveArrayJavaType;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.java.TimeZoneJavaType;
import org.hibernate.type.descriptor.java.UUIDJavaType;
import org.hibernate.type.descriptor.java.UrlJavaType;
import org.hibernate.type.descriptor.java.YearJavaType;
import org.hibernate.type.descriptor.java.ZoneIdJavaType;
import org.hibernate.type.descriptor.java.ZoneOffsetJavaType;
import org.hibernate.type.descriptor.java.ZonedDateTimeJavaType;

/**
 * Primes the {@link BaselineTarget} (which is essentially the {@link JavaTypeRegistry})
 * with Hibernate's baseline {@link JavaType} registrations
 */
public class JavaTypeBaseline {
	/**
	 * The target of the baseline registrations
	 */
	public interface BaselineTarget {
		/**
		 * Add a baseline registration
		 */
		void addBaselineDescriptor(JavaType<?> descriptor);
		/**
		 * Add a baseline registration
		 */
		void addBaselineDescriptor(Type describedJavaType, JavaType<?> descriptor);
	}

	/**
	 * The process of registering all the baseline registrations
	 */
	public static void prime(BaselineTarget target) {
		primePrimitive( target, ByteJavaType.INSTANCE );
		primePrimitive( target, BooleanJavaType.INSTANCE );
		primePrimitive( target, CharacterJavaType.INSTANCE );
		primePrimitive( target, ShortJavaType.INSTANCE );
		primePrimitive( target, IntegerJavaType.INSTANCE );
		primePrimitive( target, LongJavaType.INSTANCE );
		primePrimitive( target, FloatJavaType.INSTANCE );
		primePrimitive( target, DoubleJavaType.INSTANCE );

		target.addBaselineDescriptor( ObjectJavaType.INSTANCE );

		target.addBaselineDescriptor( BigDecimalJavaType.INSTANCE );
		target.addBaselineDescriptor( BigIntegerJavaType.INSTANCE );

		target.addBaselineDescriptor( StringJavaType.INSTANCE );

		target.addBaselineDescriptor( BlobJavaType.INSTANCE );
		target.addBaselineDescriptor( ClobJavaType.INSTANCE );
		target.addBaselineDescriptor( NClobJavaType.INSTANCE );

//		target.addBaselineDescriptor( ByteArrayJavaType.INSTANCE );
//		target.addBaselineDescriptor( CharacterArrayJavaType.INSTANCE );
		target.addBaselineDescriptor( PrimitiveByteArrayJavaType.INSTANCE );
		target.addBaselineDescriptor( PrimitiveCharacterArrayJavaType.INSTANCE );

		// Register special ArrayJavaType implementations for primitive types
		target.addBaselineDescriptor( BooleanPrimitiveArrayJavaType.INSTANCE );
		target.addBaselineDescriptor( ShortPrimitiveArrayJavaType.INSTANCE );
		target.addBaselineDescriptor( IntegerPrimitiveArrayJavaType.INSTANCE );
		target.addBaselineDescriptor( LongPrimitiveArrayJavaType.INSTANCE );
		target.addBaselineDescriptor( FloatPrimitiveArrayJavaType.INSTANCE );
		target.addBaselineDescriptor( DoublePrimitiveArrayJavaType.INSTANCE );

		target.addBaselineDescriptor( DurationJavaType.INSTANCE );
		target.addBaselineDescriptor( InstantJavaType.INSTANCE );
		target.addBaselineDescriptor( LocalDateJavaType.INSTANCE );
		target.addBaselineDescriptor( LocalDateTimeJavaType.INSTANCE );
		target.addBaselineDescriptor( LocalTimeJavaType.INSTANCE );
		target.addBaselineDescriptor( OffsetDateTimeJavaType.INSTANCE );
		target.addBaselineDescriptor( OffsetTimeJavaType.INSTANCE );
		target.addBaselineDescriptor( ZonedDateTimeJavaType.INSTANCE );
		target.addBaselineDescriptor( YearJavaType.INSTANCE );
		target.addBaselineDescriptor( ZoneIdJavaType.INSTANCE );
		target.addBaselineDescriptor( ZoneOffsetJavaType.INSTANCE );

		target.addBaselineDescriptor( CalendarJavaType.INSTANCE );
		target.addBaselineDescriptor( DateJavaType.INSTANCE );
		target.addBaselineDescriptor( java.sql.Date.class, JdbcDateJavaType.INSTANCE );
		target.addBaselineDescriptor( java.sql.Time.class, JdbcTimeJavaType.INSTANCE );
		target.addBaselineDescriptor( java.sql.Timestamp.class, JdbcTimestampJavaType.INSTANCE );
		target.addBaselineDescriptor( TimeZoneJavaType.INSTANCE );

		target.addBaselineDescriptor( ClassJavaType.INSTANCE );

		target.addBaselineDescriptor( CurrencyJavaType.INSTANCE );
		target.addBaselineDescriptor( LocaleJavaType.INSTANCE );
		target.addBaselineDescriptor( UrlJavaType.INSTANCE );
		target.addBaselineDescriptor( UUIDJavaType.INSTANCE );
		target.addBaselineDescriptor( InetAddressJavaType.INSTANCE );

		registerCollectionTypes( target );

		target.addBaselineDescriptor( MapEntryJavaType.INSTANCE );
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void registerCollectionTypes(BaselineTarget target) {
		target.addBaselineDescriptor( new CollectionJavaType( Collection.class, StandardBagSemantics.INSTANCE ) );
		target.addBaselineDescriptor( new CollectionJavaType( Object[].class, StandardArraySemantics.INSTANCE ) );
		target.addBaselineDescriptor( new CollectionJavaType( List.class, StandardListSemantics.INSTANCE ) );
		target.addBaselineDescriptor( new CollectionJavaType( Set.class, StandardSetSemantics.INSTANCE ) );
		target.addBaselineDescriptor( new CollectionJavaType( SortedSet.class, StandardSortedSetSemantics.INSTANCE ) );
		target.addBaselineDescriptor( new CollectionJavaType( Map.class, StandardMapSemantics.INSTANCE ) );
		target.addBaselineDescriptor( new CollectionJavaType( SortedMap.class, StandardSortedMapSemantics.INSTANCE ) );
	}

	private static void primePrimitive(BaselineTarget target, JavaType<?> descriptor) {
		target.addBaselineDescriptor( descriptor );
		target.addBaselineDescriptor( ( (PrimitiveJavaType<?>) descriptor ).getPrimitiveClass(), descriptor );
	}
}
