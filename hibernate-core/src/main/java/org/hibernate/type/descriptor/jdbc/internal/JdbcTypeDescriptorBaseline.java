/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.jdbc.internal;

import java.sql.Types;

import org.hibernate.type.descriptor.jdbc.BigIntTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.BinaryTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.BlobTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.BooleanTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.CharTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.ClobTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.DateTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.DecimalTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.DoubleTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.FloatTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.IntegerTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.LongVarbinaryTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.LongVarcharTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.NumericTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.RealTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.SmallIntTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.TimeTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.TimestampTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.TimestampWithTimeZoneDescriptor;
import org.hibernate.type.descriptor.jdbc.TinyIntTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.VarbinaryTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.VarcharTypeDescriptor;

/**
 * Registers the base {@link JdbcTypeDescriptor} instances.
 *
 * @author Chris Cranford
 */
public class JdbcTypeDescriptorBaseline {
	public interface BaselineTarget {
		void addDescriptor(JdbcTypeDescriptor descriptor);
		void addDescriptor(int code, JdbcTypeDescriptor descriptor);
	}

	public static void prime(BaselineTarget target) {
		target.addDescriptor( BooleanTypeDescriptor.INSTANCE );
		// ResultSetMetaData might report BIT on some DBs, so we need to register the boolean type descriptor for that code
		target.addDescriptor( Types.BIT, BooleanTypeDescriptor.INSTANCE );
		target.addDescriptor( BigIntTypeDescriptor.INSTANCE );
		target.addDescriptor( DecimalTypeDescriptor.INSTANCE );
		target.addDescriptor( DoubleTypeDescriptor.INSTANCE );
		target.addDescriptor( FloatTypeDescriptor.INSTANCE );
		target.addDescriptor( IntegerTypeDescriptor.INSTANCE );
		target.addDescriptor( NumericTypeDescriptor.INSTANCE );
		target.addDescriptor( RealTypeDescriptor.INSTANCE );
		target.addDescriptor( SmallIntTypeDescriptor.INSTANCE );
		target.addDescriptor( TinyIntTypeDescriptor.INSTANCE );

		target.addDescriptor( DateTypeDescriptor.INSTANCE );
		target.addDescriptor( TimestampTypeDescriptor.INSTANCE );
		target.addDescriptor( TimestampWithTimeZoneDescriptor.INSTANCE );
		target.addDescriptor( TimeTypeDescriptor.INSTANCE );

		target.addDescriptor( BinaryTypeDescriptor.INSTANCE );
		target.addDescriptor( VarbinaryTypeDescriptor.INSTANCE );
		target.addDescriptor( LongVarbinaryTypeDescriptor.INSTANCE );

		target.addDescriptor( CharTypeDescriptor.INSTANCE );
		target.addDescriptor( VarcharTypeDescriptor.INSTANCE );
		target.addDescriptor( LongVarcharTypeDescriptor.INSTANCE );

		target.addDescriptor( BlobTypeDescriptor.DEFAULT );
		target.addDescriptor( ClobTypeDescriptor.DEFAULT );

		// Assume `NationalizationSupport#IMPLICIT`.  Dialects needing the
		// explicit type will map them..
		target.addDescriptor( Types.NCHAR, CharTypeDescriptor.INSTANCE );
		target.addDescriptor( Types.NVARCHAR, VarcharTypeDescriptor.INSTANCE );
		target.addDescriptor( Types.LONGNVARCHAR, LongVarcharTypeDescriptor.INSTANCE );
		target.addDescriptor( Types.NCLOB, ClobTypeDescriptor.DEFAULT );
	}
}
