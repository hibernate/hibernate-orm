/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.sql.internal;

import org.hibernate.type.descriptor.sql.BigIntTypeDescriptor;
import org.hibernate.type.descriptor.sql.BinaryTypeDescriptor;
import org.hibernate.type.descriptor.sql.BitTypeDescriptor;
import org.hibernate.type.descriptor.sql.BlobTypeDescriptor;
import org.hibernate.type.descriptor.sql.BooleanTypeDescriptor;
import org.hibernate.type.descriptor.sql.CharTypeDescriptor;
import org.hibernate.type.descriptor.sql.ClobTypeDescriptor;
import org.hibernate.type.descriptor.sql.DateTypeDescriptor;
import org.hibernate.type.descriptor.sql.DecimalTypeDescriptor;
import org.hibernate.type.descriptor.sql.DoubleTypeDescriptor;
import org.hibernate.type.descriptor.sql.FloatTypeDescriptor;
import org.hibernate.type.descriptor.sql.IntegerTypeDescriptor;
import org.hibernate.type.descriptor.sql.LongNVarcharTypeDescriptor;
import org.hibernate.type.descriptor.sql.LongVarbinaryTypeDescriptor;
import org.hibernate.type.descriptor.sql.LongVarcharTypeDescriptor;
import org.hibernate.type.descriptor.sql.NCharTypeDescriptor;
import org.hibernate.type.descriptor.sql.NClobTypeDescriptor;
import org.hibernate.type.descriptor.sql.NVarcharTypeDescriptor;
import org.hibernate.type.descriptor.sql.NumericTypeDescriptor;
import org.hibernate.type.descriptor.sql.RealTypeDescriptor;
import org.hibernate.type.descriptor.sql.SmallIntTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.type.descriptor.sql.TimeTypeDescriptor;
import org.hibernate.type.descriptor.sql.TimestampTypeDescriptor;
import org.hibernate.type.descriptor.sql.TinyIntTypeDescriptor;
import org.hibernate.type.descriptor.sql.VarbinaryTypeDescriptor;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

/**
 * Registers the base {@link SqlTypeDescriptor} instances.
 *
 * @author Chris Cranford
 */
public class SqlTypeDescriptorBaseline {
	public interface BaselineTarget {
		void addDescriptor(SqlTypeDescriptor descriptor);
	}

	public static void prime(BaselineTarget target) {
		target.addDescriptor( BooleanTypeDescriptor .INSTANCE );

		target.addDescriptor( BitTypeDescriptor.INSTANCE );
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
		target.addDescriptor( TimeTypeDescriptor.INSTANCE );

		target.addDescriptor( BinaryTypeDescriptor.INSTANCE );
		target.addDescriptor( VarbinaryTypeDescriptor.INSTANCE );
		target.addDescriptor( LongVarbinaryTypeDescriptor.INSTANCE );

		target.addDescriptor( CharTypeDescriptor.INSTANCE );
		target.addDescriptor( VarcharTypeDescriptor.INSTANCE );
		target.addDescriptor( LongVarcharTypeDescriptor.INSTANCE );

		target.addDescriptor( NCharTypeDescriptor.INSTANCE );
		target.addDescriptor( NVarcharTypeDescriptor.INSTANCE );
		target.addDescriptor( LongNVarcharTypeDescriptor.INSTANCE );

		// Use the default LOB mappings by default
		target.addDescriptor( BlobTypeDescriptor.DEFAULT );
		target.addDescriptor( ClobTypeDescriptor.DEFAULT );
		target.addDescriptor( NClobTypeDescriptor.DEFAULT );
	}
}
