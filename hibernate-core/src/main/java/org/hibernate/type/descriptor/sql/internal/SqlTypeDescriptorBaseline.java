/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.sql.internal;

import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.descriptor.sql.spi.BigIntSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.BinarySqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.BitSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.BlobSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.BooleanSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.CharSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.ClobSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.DateSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.DecimalSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.DoubleSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.FloatSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.IntegerSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.LongNVarcharSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.LongVarbinarySqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.LongVarcharSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.NCharSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.NClobSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.NVarcharSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.NumericSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.RealSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.SmallIntSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.TimeSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.TimestampSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.TinyIntSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.VarbinarySqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.VarcharSqlDescriptor;

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
		target.addDescriptor( BooleanSqlDescriptor.INSTANCE );

		target.addDescriptor( BitSqlDescriptor.INSTANCE );
		target.addDescriptor( BigIntSqlDescriptor.INSTANCE );
		target.addDescriptor( DecimalSqlDescriptor.INSTANCE );
		target.addDescriptor( DoubleSqlDescriptor.INSTANCE );
		target.addDescriptor( FloatSqlDescriptor.INSTANCE );
		target.addDescriptor( IntegerSqlDescriptor.INSTANCE );
		target.addDescriptor( NumericSqlDescriptor.INSTANCE );
		target.addDescriptor( RealSqlDescriptor.INSTANCE );
		target.addDescriptor( SmallIntSqlDescriptor.INSTANCE );
		target.addDescriptor( TinyIntSqlDescriptor.INSTANCE );

		target.addDescriptor( DateSqlDescriptor.INSTANCE );
		target.addDescriptor( TimestampSqlDescriptor.INSTANCE );
		target.addDescriptor( TimeSqlDescriptor.INSTANCE );

		target.addDescriptor( BinarySqlDescriptor.INSTANCE );
		target.addDescriptor( VarbinarySqlDescriptor.INSTANCE );
		target.addDescriptor( LongVarbinarySqlDescriptor.INSTANCE );
		target.addDescriptor( BlobSqlDescriptor.DEFAULT );

		target.addDescriptor( CharSqlDescriptor.INSTANCE );
		target.addDescriptor( VarcharSqlDescriptor.INSTANCE );
		target.addDescriptor( LongVarcharSqlDescriptor.INSTANCE );
		target.addDescriptor( ClobSqlDescriptor.DEFAULT );

		target.addDescriptor( NCharSqlDescriptor.INSTANCE );
		target.addDescriptor( NVarcharSqlDescriptor.INSTANCE );
		target.addDescriptor( LongNVarcharSqlDescriptor.INSTANCE );
		target.addDescriptor( NClobSqlDescriptor.DEFAULT );
	}
}
