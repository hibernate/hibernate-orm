/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.jdbc.internal;

import java.sql.Types;

import org.hibernate.type.descriptor.jdbc.BigIntJdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.BinaryJdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.BlobJdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.BooleanJdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.CharJdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.ClobJdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.DateJdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.DecimalJdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.DoubleJdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.FloatJdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.IntegerJdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.LongVarbinaryJdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.LongVarcharJdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.NumericJdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.RealJdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.SmallIntJdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.TimeJdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.TimestampJdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.TimestampWithTimeZoneJdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.TinyIntJdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.VarbinaryJdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcTypeDescriptor;

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
		target.addDescriptor( BooleanJdbcTypeDescriptor.INSTANCE );
		// ResultSetMetaData might report BIT on some DBs, so we need to register the boolean type descriptor for that code
		target.addDescriptor( Types.BIT, BooleanJdbcTypeDescriptor.INSTANCE );
		target.addDescriptor( BigIntJdbcTypeDescriptor.INSTANCE );
		target.addDescriptor( DecimalJdbcTypeDescriptor.INSTANCE );
		target.addDescriptor( DoubleJdbcTypeDescriptor.INSTANCE );
		target.addDescriptor( FloatJdbcTypeDescriptor.INSTANCE );
		target.addDescriptor( IntegerJdbcTypeDescriptor.INSTANCE );
		target.addDescriptor( NumericJdbcTypeDescriptor.INSTANCE );
		target.addDescriptor( RealJdbcTypeDescriptor.INSTANCE );
		target.addDescriptor( SmallIntJdbcTypeDescriptor.INSTANCE );
		target.addDescriptor( TinyIntJdbcTypeDescriptor.INSTANCE );

		target.addDescriptor( DateJdbcTypeDescriptor.INSTANCE );
		target.addDescriptor( TimestampJdbcTypeDescriptor.INSTANCE );
		target.addDescriptor( TimestampWithTimeZoneJdbcTypeDescriptor.INSTANCE );
		target.addDescriptor( TimeJdbcTypeDescriptor.INSTANCE );

		target.addDescriptor( BinaryJdbcTypeDescriptor.INSTANCE );
		target.addDescriptor( VarbinaryJdbcTypeDescriptor.INSTANCE );
		target.addDescriptor( LongVarbinaryJdbcTypeDescriptor.INSTANCE );

		target.addDescriptor( CharJdbcTypeDescriptor.INSTANCE );
		target.addDescriptor( VarcharJdbcTypeDescriptor.INSTANCE );
		target.addDescriptor( LongVarcharJdbcTypeDescriptor.INSTANCE );

		target.addDescriptor( BlobJdbcTypeDescriptor.DEFAULT );
		target.addDescriptor( ClobJdbcTypeDescriptor.DEFAULT );

		// Assume `NationalizationSupport#IMPLICIT`.  Dialects needing the
		// explicit type will map them..
		target.addDescriptor( Types.NCHAR, CharJdbcTypeDescriptor.INSTANCE );
		target.addDescriptor( Types.NVARCHAR, VarcharJdbcTypeDescriptor.INSTANCE );
		target.addDescriptor( Types.LONGNVARCHAR, LongVarcharJdbcTypeDescriptor.INSTANCE );
		target.addDescriptor( Types.NCLOB, ClobJdbcTypeDescriptor.DEFAULT );
	}
}
