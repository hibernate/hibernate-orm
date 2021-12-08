/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java.spi;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.hibernate.collection.internal.StandardArraySemantics;
import org.hibernate.collection.internal.StandardBagSemantics;
import org.hibernate.collection.internal.StandardListSemantics;
import org.hibernate.collection.internal.StandardMapSemantics;
import org.hibernate.collection.internal.StandardOrderedMapSemantics;
import org.hibernate.collection.internal.StandardOrderedSetSemantics;
import org.hibernate.collection.internal.StandardSetSemantics;
import org.hibernate.collection.internal.StandardSortedMapSemantics;
import org.hibernate.collection.internal.StandardSortedSetSemantics;
import org.hibernate.type.descriptor.java.BigDecimalJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.BigIntegerJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.BlobJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.BooleanJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.ByteArrayJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.ByteJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.CalendarJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.CharacterArrayJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.CharacterJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.ClassJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.ClobJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.CurrencyJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.DateJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.DoubleJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.DurationJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.FloatTypeDescriptor;
import org.hibernate.type.descriptor.java.InetAddressJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.InstantJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.IntegerJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.ObjectJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.JdbcDateJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.JdbcTimeJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.JdbcTimestampJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.LocalDateJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.LocalDateTimeJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.LocalTimeJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.LocaleJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.LongJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.NClobJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.OffsetDateTimeJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.OffsetTimeJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.PrimitiveCharacterArrayJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.ShortJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.StringJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.TimeZoneJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.UUIDJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.UrlJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.YearJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.ZoneIdJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.ZoneOffsetJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.ZonedDateTimeJavaTypeDescriptor;

/**
 * Primes the {@link BaselineTarget} (which is essentially the {@link JavaTypeRegistry})
 * with Hibernate's baseline {@link JavaType} registrations
 */
public class JavaTypeDescriptorBaseline {
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
	@SuppressWarnings("unchecked")
	public static void prime(BaselineTarget target) {
		primePrimitive( target, ByteJavaTypeDescriptor.INSTANCE );
		primePrimitive( target, BooleanJavaTypeDescriptor.INSTANCE );
		primePrimitive( target, CharacterJavaTypeDescriptor.INSTANCE );
		primePrimitive( target, ShortJavaTypeDescriptor.INSTANCE );
		primePrimitive( target, IntegerJavaTypeDescriptor.INSTANCE );
		primePrimitive( target, LongJavaTypeDescriptor.INSTANCE );
		primePrimitive( target, FloatTypeDescriptor.INSTANCE );
		primePrimitive( target, DoubleJavaTypeDescriptor.INSTANCE );

		target.addBaselineDescriptor( ObjectJavaTypeDescriptor.INSTANCE );

		target.addBaselineDescriptor( BigDecimalJavaTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( BigIntegerJavaTypeDescriptor.INSTANCE );

		target.addBaselineDescriptor( StringJavaTypeDescriptor.INSTANCE );

		target.addBaselineDescriptor( BlobJavaTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( ClobJavaTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( NClobJavaTypeDescriptor.INSTANCE );

		target.addBaselineDescriptor( ByteArrayJavaTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( CharacterArrayJavaTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( PrimitiveByteArrayJavaTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( PrimitiveCharacterArrayJavaTypeDescriptor.INSTANCE );

		target.addBaselineDescriptor( DurationJavaTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( InstantJavaTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( LocalDateJavaTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( LocalDateTimeJavaTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( LocalTimeJavaTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( OffsetDateTimeJavaTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( OffsetTimeJavaTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( ZonedDateTimeJavaTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( YearJavaTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( ZoneIdJavaTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( ZoneOffsetJavaTypeDescriptor.INSTANCE );

		target.addBaselineDescriptor( CalendarJavaTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( DateJavaTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( java.sql.Date.class, JdbcDateJavaTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( java.sql.Time.class, JdbcTimeJavaTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( java.sql.Timestamp.class, JdbcTimestampJavaTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( TimeZoneJavaTypeDescriptor.INSTANCE );

		target.addBaselineDescriptor( ClassJavaTypeDescriptor.INSTANCE );

		target.addBaselineDescriptor( CurrencyJavaTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( LocaleJavaTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( UrlJavaTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( UUIDJavaTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( InetAddressJavaTypeDescriptor.INSTANCE );

		registerCollectionTypes( target );

		target.addBaselineDescriptor( MapEntryJavaDescriptor.INSTANCE );
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void registerCollectionTypes(BaselineTarget target) {
		target.addBaselineDescriptor( new CollectionJavaTypeDescriptor( Collection.class, StandardBagSemantics.INSTANCE ) );
		target.addBaselineDescriptor( new CollectionJavaTypeDescriptor( Object[].class, StandardArraySemantics.INSTANCE ) );
		target.addBaselineDescriptor( new CollectionJavaTypeDescriptor( List.class, StandardListSemantics.INSTANCE ) );
		target.addBaselineDescriptor( new CollectionJavaTypeDescriptor( ArrayList.class, StandardListSemantics.INSTANCE ) );
		target.addBaselineDescriptor( new CollectionJavaTypeDescriptor( Set.class, StandardSetSemantics.INSTANCE ) );
		target.addBaselineDescriptor( new CollectionJavaTypeDescriptor( HashSet.class, StandardSetSemantics.INSTANCE ) );
		target.addBaselineDescriptor( new CollectionJavaTypeDescriptor( SortedSet.class, StandardSortedSetSemantics.INSTANCE ) );
		target.addBaselineDescriptor( new CollectionJavaTypeDescriptor( TreeSet.class, StandardOrderedSetSemantics.INSTANCE ) );
		target.addBaselineDescriptor( new CollectionJavaTypeDescriptor( LinkedHashSet.class, StandardOrderedSetSemantics.INSTANCE ) );
		target.addBaselineDescriptor( new CollectionJavaTypeDescriptor( Map.class, StandardMapSemantics.INSTANCE ) );
		target.addBaselineDescriptor( new CollectionJavaTypeDescriptor( HashMap.class, StandardMapSemantics.INSTANCE ) );
		target.addBaselineDescriptor( new CollectionJavaTypeDescriptor( SortedMap.class, StandardSortedMapSemantics.INSTANCE ) );
		target.addBaselineDescriptor( new CollectionJavaTypeDescriptor( TreeMap.class, StandardSortedMapSemantics.INSTANCE ) );
		target.addBaselineDescriptor( new CollectionJavaTypeDescriptor( LinkedHashMap.class, StandardOrderedMapSemantics.INSTANCE ) );
	}

	private static void primePrimitive(BaselineTarget target, JavaType descriptor) {
		target.addBaselineDescriptor( descriptor );
		target.addBaselineDescriptor( ( (PrimitiveJavaType) descriptor ).getPrimitiveClass(), descriptor );
	}
}
