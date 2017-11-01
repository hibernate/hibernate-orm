/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.internal;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.Primitive;

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
		primePrimitive( target, ByteJavaDescriptor.INSTANCE );
		primePrimitive( target, BooleanJavaDescriptor.INSTANCE );
		primePrimitive( target, CharacterJavaDescriptor.INSTANCE );
		primePrimitive( target, ShortJavaDescriptor.INSTANCE );
		primePrimitive( target, IntegerJavaDescriptor.INSTANCE );
		primePrimitive( target, LongJavaDescriptor.INSTANCE );
		primePrimitive( target, FloatJavaDescriptor.INSTANCE );
		primePrimitive( target, DoubleJavaDescriptor.INSTANCE );

		target.addBaselineDescriptor( BigDecimalJavaDescriptor.INSTANCE );
		target.addBaselineDescriptor( BigIntegerJavaDescriptor.INSTANCE );

		target.addBaselineDescriptor( StringJavaDescriptor.INSTANCE );

		target.addBaselineDescriptor( BlobJavaDescriptor.INSTANCE );
		target.addBaselineDescriptor( ClobJavaDescriptor.INSTANCE );
		target.addBaselineDescriptor( NClobJavaDescriptor.INSTANCE );

		target.addBaselineDescriptor( ByteArrayJavaDescriptor.INSTANCE );
		target.addBaselineDescriptor( CharacterArrayJavaDescriptor.INSTANCE );
		target.addBaselineDescriptor( PrimitiveByteArrayJavaDescriptor.INSTANCE );
		target.addBaselineDescriptor( PrimitiveCharacterArrayJavaDescriptor.INSTANCE );

		target.addBaselineDescriptor( DurationJavaDescriptor.INSTANCE );
		target.addBaselineDescriptor( InstantJavaDescriptor.INSTANCE );
		target.addBaselineDescriptor( LocalDateJavaDescriptor.INSTANCE );
		target.addBaselineDescriptor( LocalDateTimeJavaDescriptor.INSTANCE );
		target.addBaselineDescriptor( OffsetDateTimeJavaDescriptor.INSTANCE );
		target.addBaselineDescriptor( OffsetTimeJavaDescriptor.INSTANCE );
		target.addBaselineDescriptor( ZonedDateTimeJavaDescriptor.INSTANCE );

		target.addBaselineDescriptor( CalendarJavaDescriptor.INSTANCE );
		target.addBaselineDescriptor( DateJavaDescriptor.INSTANCE );
		target.addBaselineDescriptor( java.sql.Date.class, JdbcDateJavaDescriptor.INSTANCE );
		target.addBaselineDescriptor( java.sql.Time.class, JdbcTimeJavaDescriptor.INSTANCE );
		target.addBaselineDescriptor( java.sql.Timestamp.class, JdbcTimestampJavaDescriptor.INSTANCE );
		target.addBaselineDescriptor( TimeZoneJavaDescriptor.INSTANCE );

		target.addBaselineDescriptor( ClassJavaDescriptor.INSTANCE );

		target.addBaselineDescriptor( CurrencyJavaDescriptor.INSTANCE );
		target.addBaselineDescriptor( LocaleJavaDescriptor.INSTANCE );
		target.addBaselineDescriptor( UrlJavaDescriptor.INSTANCE );
		target.addBaselineDescriptor( UUIDJavaDescriptor.INSTANCE );

		target.addBaselineDescriptor( new CollectionJavaDescriptor( Collection.class ) );
		target.addBaselineDescriptor( new CollectionJavaDescriptor( List.class ) );
		target.addBaselineDescriptor( new CollectionJavaDescriptor( Set.class ) );
		target.addBaselineDescriptor( new CollectionJavaDescriptor( SortedSet.class ) );
		target.addBaselineDescriptor( new CollectionJavaDescriptor( Map.class ) );
		target.addBaselineDescriptor( new CollectionJavaDescriptor( SortedMap.class ) );
		target.addBaselineDescriptor( MapEntryJavaDescriptor.INSTANCE );

	}

	private static void primePrimitive(BaselineTarget target, BasicJavaDescriptor descriptor) {
		target.addBaselineDescriptor( descriptor );
		target.addBaselineDescriptor( ( (Primitive) descriptor ).getPrimitiveClass(), descriptor );
	}
}