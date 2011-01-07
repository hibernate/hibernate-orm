/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.type;

import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayTypeDescriptor;
import org.hibernate.type.descriptor.sql.BasicBinder;
import org.hibernate.type.descriptor.sql.BasicExtractor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * Specialized type mapping that can override {@link MaterializedBlobType}
 * to create a {@link java.sql.Blob} from the byte[] for binding via
 * {@link java.sql.PreparedStatement#setBlob}, and to extract byte[] from a
 * {@link java.sql.Blob} returned by {@link java.sql.ResultSet#getBlob}.
 *
 * It can be used when stream, {@link java.sql.PreparedStatement#setBytes(int, byte[])},
 * or {@link java.sql.ResultSet#getBytes} cannot be used for Blob columns.
 * 
 * @author Gail Badner
 */
public class PostgresMaterializedBlobType extends AbstractSingleColumnStandardBasicType<byte[]> {
	public static final PostgresMaterializedBlobType INSTANCE = new PostgresMaterializedBlobType();

	public PostgresMaterializedBlobType() {
		super( PostgresMaterializedBlobTypeDescriptor.INSTANCE, PrimitiveByteArrayTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return MaterializedBlobType.INSTANCE.getName();
	}

	public static class PostgresMaterializedBlobTypeDescriptor implements SqlTypeDescriptor {
		public static final PostgresMaterializedBlobTypeDescriptor INSTANCE = new PostgresMaterializedBlobTypeDescriptor();

		public int getSqlType() {
			return Types.BLOB;
		}

		public <X> ValueBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
			return new BasicBinder<X>( javaTypeDescriptor, this ) {
				@Override
				protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
						st.setBlob( index, javaTypeDescriptor.unwrap( value, Blob.class, options ) );
				}
			};
		}

		public <X> ValueExtractor<X> getExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
			return new BasicExtractor<X>( javaTypeDescriptor, this ) {
				@Override
				protected X doExtract(ResultSet rs, String name, WrapperOptions options) throws SQLException {
					return javaTypeDescriptor.wrap( rs.getBlob( name ), options );
				}
			};
		}
	}
}
