/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.type.descriptor.sql;

import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.engine.jdbc.BinaryStream;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Descriptor for {@link Types#BLOB BLOB} handling.
 *
 * @author Steve Ebersole
 * @author Gail Badner
 * @author Brett Meyer
 */
public abstract class BlobTypeDescriptor implements SqlTypeDescriptor {

	private BlobTypeDescriptor() {
	}

	@Override
	public int getSqlType() {
		return Types.BLOB;
	}

	@Override
	public boolean canBeRemapped() {
		return true;
	}

	protected abstract <X> BasicExtractor<X> getBlobExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor);

	@Override
	public <X> BasicExtractor<X> getExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return getBlobExtractor( javaTypeDescriptor );
	}

	protected abstract <X> BasicBinder<X> getBlobBinder(final JavaTypeDescriptor<X> javaTypeDescriptor);

	public <X> BasicBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return getBlobBinder( javaTypeDescriptor );
	}

	public static final BlobTypeDescriptor DEFAULT =
			new BlobTypeDescriptor() {
				@Override
                public <X> BasicBinder<X> getBlobBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
					return new BasicBinder<X>( javaTypeDescriptor, this ) {
						@Override
						protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
							BlobTypeDescriptor descriptor = BLOB_BINDING;
							if ( options.useStreamForLobBinding() ) {
								descriptor = STREAM_BINDING;
							}
							else if ( byte[].class.isInstance( value ) ) {
								// performance shortcut for binding BLOB data in byte[] format
								descriptor = PRIMITIVE_ARRAY_BINDING;
							}
							descriptor.getBlobBinder( javaTypeDescriptor ).doBind( st, value, index, options );
						}
					};
				}

				@Override
                public <X> BasicExtractor<X> getBlobExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
					return new BasicExtractor<X>( javaTypeDescriptor, this ) {
						// For now, default to using getBlob.  If extraction
						// should also check useStreamForLobBinding, add
						// checks here and use STREAM_BINDING.
						
						@Override
						protected X doExtract(ResultSet rs, String name, WrapperOptions options) throws SQLException {
							return BLOB_BINDING.getExtractor( javaTypeDescriptor ).doExtract( rs, name, options );
						}
					};
				}
			};

	public static final BlobTypeDescriptor PRIMITIVE_ARRAY_BINDING =
			new BlobTypeDescriptor() {
				@Override
                public <X> BasicBinder<X> getBlobBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
					return new BasicBinder<X>( javaTypeDescriptor, this ) {
						@Override
						public void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
								throws SQLException {
							st.setBytes( index, javaTypeDescriptor.unwrap( value, byte[].class, options ) );
						}
					};
				}

				@Override
                public <X> BasicExtractor<X> getBlobExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
					return new BasicExtractor<X>( javaTypeDescriptor, this ) {
						@Override
						protected X doExtract(ResultSet rs, String name, WrapperOptions options) throws SQLException {
							return javaTypeDescriptor.wrap( rs.getBytes( name ), options );
						}
					};
				}
			};

	public static final BlobTypeDescriptor BLOB_BINDING =
			new BlobTypeDescriptor() {
				@Override
                public <X> BasicBinder<X> getBlobBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
					return new BasicBinder<X>( javaTypeDescriptor, this ) {
						@Override
						protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
								throws SQLException {
							st.setBlob( index, javaTypeDescriptor.unwrap( value, Blob.class, options ) );
						}
					};
				}

				@Override
                public <X> BasicExtractor<X> getBlobExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
					return new BasicExtractor<X>( javaTypeDescriptor, this ) {
						@Override
						protected X doExtract(ResultSet rs, String name, WrapperOptions options) throws SQLException {
							return javaTypeDescriptor.wrap( rs.getBlob( name ), options );
						}
					};
				}
			};

	public static final BlobTypeDescriptor STREAM_BINDING =
			new BlobTypeDescriptor() {
				@Override
                public <X> BasicBinder<X> getBlobBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
					return new BasicBinder<X>( javaTypeDescriptor, this ) {
						@Override
						protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
								throws SQLException {
							final BinaryStream binaryStream = javaTypeDescriptor.unwrap( value, BinaryStream.class, options );
							st.setBinaryStream( index, binaryStream.getInputStream(), binaryStream.getLength() );
						}
					};
				}

				@Override
                public <X> BasicExtractor<X> getBlobExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
					return new BasicExtractor<X>( javaTypeDescriptor, this ) {
						@Override
						protected X doExtract(ResultSet rs, String name, WrapperOptions options) throws SQLException {
							return javaTypeDescriptor.wrap( rs.getBinaryStream( name ), options );
						}
					};
				}
			};
}
