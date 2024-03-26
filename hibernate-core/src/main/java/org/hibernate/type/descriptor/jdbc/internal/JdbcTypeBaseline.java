/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.jdbc.internal;

import java.sql.Types;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.BigIntJdbcType;
import org.hibernate.type.descriptor.jdbc.BinaryJdbcType;
import org.hibernate.type.descriptor.jdbc.BlobJdbcType;
import org.hibernate.type.descriptor.jdbc.BooleanJdbcType;
import org.hibernate.type.descriptor.jdbc.CharJdbcType;
import org.hibernate.type.descriptor.jdbc.ClobJdbcType;
import org.hibernate.type.descriptor.jdbc.DateJdbcType;
import org.hibernate.type.descriptor.jdbc.DecimalJdbcType;
import org.hibernate.type.descriptor.jdbc.DoubleJdbcType;
import org.hibernate.type.descriptor.jdbc.DurationJdbcType;
import org.hibernate.type.descriptor.jdbc.FloatJdbcType;
import org.hibernate.type.descriptor.jdbc.InstantJdbcType;
import org.hibernate.type.descriptor.jdbc.IntegerJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.LocalDateJdbcType;
import org.hibernate.type.descriptor.jdbc.LocalDateTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.LocalTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.LongVarbinaryJdbcType;
import org.hibernate.type.descriptor.jdbc.LongVarcharJdbcType;
import org.hibernate.type.descriptor.jdbc.NumericJdbcType;
import org.hibernate.type.descriptor.jdbc.OffsetDateTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.OffsetTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.RealJdbcType;
import org.hibernate.type.descriptor.jdbc.RowIdJdbcType;
import org.hibernate.type.descriptor.jdbc.SmallIntJdbcType;
import org.hibernate.type.descriptor.jdbc.TimeJdbcType;
import org.hibernate.type.descriptor.jdbc.TimeWithTimeZoneJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampWithTimeZoneJdbcType;
import org.hibernate.type.descriptor.jdbc.TinyIntJdbcType;
import org.hibernate.type.descriptor.jdbc.VarbinaryJdbcType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.hibernate.type.descriptor.jdbc.ZonedDateTimeJdbcType;

/**
 * Registers the base {@link JdbcType} instances.
 *
 * @author Chris Cranford
 */
public class JdbcTypeBaseline {
	public interface BaselineTarget {
		void addDescriptor(JdbcType descriptor);
		void addDescriptor(int code, JdbcType descriptor);
	}

	public static void prime(BaselineTarget target) {
		target.addDescriptor( BooleanJdbcType.INSTANCE );
		// ResultSetMetaData might report BIT on some DBs, so we need to register the boolean type descriptor for that code
		target.addDescriptor( Types.BIT, BooleanJdbcType.INSTANCE );
		target.addDescriptor( BigIntJdbcType.INSTANCE );
		target.addDescriptor( DecimalJdbcType.INSTANCE );
		target.addDescriptor( DoubleJdbcType.INSTANCE );
		target.addDescriptor( SqlTypes.DOUBLE, DoubleJdbcType.INSTANCE );
		target.addDescriptor( FloatJdbcType.INSTANCE );
		target.addDescriptor( IntegerJdbcType.INSTANCE );
		target.addDescriptor( NumericJdbcType.INSTANCE );
		target.addDescriptor( RealJdbcType.INSTANCE );
		target.addDescriptor( SmallIntJdbcType.INSTANCE );
		target.addDescriptor( TinyIntJdbcType.INSTANCE );

		target.addDescriptor( InstantJdbcType.INSTANCE );
		target.addDescriptor( LocalDateTimeJdbcType.INSTANCE );
		target.addDescriptor( LocalDateJdbcType.INSTANCE );
		target.addDescriptor( LocalTimeJdbcType.INSTANCE );
		target.addDescriptor( OffsetDateTimeJdbcType.INSTANCE );
		target.addDescriptor( OffsetTimeJdbcType.INSTANCE );
		target.addDescriptor( ZonedDateTimeJdbcType.INSTANCE );

		target.addDescriptor( DateJdbcType.INSTANCE );
		target.addDescriptor( TimestampJdbcType.INSTANCE );
		target.addDescriptor( TimestampWithTimeZoneJdbcType.INSTANCE );
		target.addDescriptor( TimeJdbcType.INSTANCE );
		target.addDescriptor( TimeWithTimeZoneJdbcType.INSTANCE );
		target.addDescriptor( DurationJdbcType.INSTANCE );

		target.addDescriptor( BinaryJdbcType.INSTANCE );
		target.addDescriptor( VarbinaryJdbcType.INSTANCE );
		target.addDescriptor( LongVarbinaryJdbcType.INSTANCE );
		target.addDescriptor( new LongVarbinaryJdbcType(SqlTypes.LONG32VARBINARY) );

		target.addDescriptor( CharJdbcType.INSTANCE );
		target.addDescriptor( VarcharJdbcType.INSTANCE );
		target.addDescriptor( LongVarcharJdbcType.INSTANCE );
		target.addDescriptor( new LongVarcharJdbcType(SqlTypes.LONG32VARCHAR) );

		target.addDescriptor( BlobJdbcType.DEFAULT );
		target.addDescriptor( ClobJdbcType.DEFAULT );

		// Assume `NationalizationSupport#IMPLICIT`.  Dialects needing the
		// explicit type will map them.
		target.addDescriptor( Types.NCHAR, CharJdbcType.INSTANCE );
		target.addDescriptor( Types.NVARCHAR, VarcharJdbcType.INSTANCE );
		target.addDescriptor( Types.LONGNVARCHAR, LongVarcharJdbcType.INSTANCE );
		target.addDescriptor( Types.NCLOB, ClobJdbcType.DEFAULT );
		target.addDescriptor( new LongVarcharJdbcType(SqlTypes.LONG32NVARCHAR) );

		target.addDescriptor( RowIdJdbcType.INSTANCE );
	}
}
