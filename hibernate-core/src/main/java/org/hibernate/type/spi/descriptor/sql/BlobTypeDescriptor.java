/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi.descriptor.sql;

import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.engine.jdbc.BinaryStream;
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.spi.descriptor.ValueExtractor;
import org.hibernate.type.spi.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

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

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaTypeDescriptor<T> javaTypeDescriptor) {
		// literal values for BLOB data is not supported.
		return null;
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicExtractor<X>( javaTypeDescriptor, this ) {
			@Override
			protected X doExtract(ResultSet rs, String name, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( rs.getBlob( name ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( statement.getBlob( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				return javaTypeDescriptor.wrap( statement.getBlob( name ), options );
			}
		};
	}

	protected abstract <X> BasicBinder<X> getBlobBinder(final JavaTypeDescriptor<X> javaTypeDescriptor);

	@Override
	public <X> BasicBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return getBlobBinder( javaTypeDescriptor );
	}

	public static final BlobTypeDescriptor DEFAULT = new BlobTypeDescriptor() {
		@Override
		public JavaTypeDescriptor getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
			return typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( Blob.class );
		}

		@Override
		public <X> BasicBinder<X> getBlobBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
			return new BasicBinder<X>( javaTypeDescriptor, this ) {
				@Override
				protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
						throws SQLException {
					BlobTypeDescriptor descriptor = BLOB_BINDING;
					if ( byte[].class.isInstance( value ) ) {
						// performance shortcut for binding BLOB data in byte[] format
						descriptor = PRIMITIVE_ARRAY_BINDING;
					}
					else if ( options.useStreamForLobBinding() ) {
						descriptor = STREAM_BINDING;
					}
					descriptor.getBlobBinder( javaTypeDescriptor ).doBind( st, value, index, options );
				}

				@Override
				protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
						throws SQLException {
					BlobTypeDescriptor descriptor = BLOB_BINDING;
					if ( byte[].class.isInstance( value ) ) {
						// performance shortcut for binding BLOB data in byte[] format
						descriptor = PRIMITIVE_ARRAY_BINDING;
					}
					else if ( options.useStreamForLobBinding() ) {
						descriptor = STREAM_BINDING;
					}
					descriptor.getBlobBinder( javaTypeDescriptor ).doBind( st, value, name, options );
				}
			};
		}
	};

	public static final BlobTypeDescriptor PRIMITIVE_ARRAY_BINDING = new BlobTypeDescriptor() {
		@Override
		public JavaTypeDescriptor getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
			return typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( byte[].class );
		}

		@Override
		public <X> BasicBinder<X> getBlobBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
			return new BasicBinder<X>( javaTypeDescriptor, this ) {
				@Override
				public void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
						throws SQLException {
					st.setBytes( index, javaTypeDescriptor.unwrap( value, byte[].class, options ) );
				}

				@Override
				protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
						throws SQLException {
					st.setBytes( name, javaTypeDescriptor.unwrap( value, byte[].class, options ) );
				}
			};
		}
	};

	public static final BlobTypeDescriptor BLOB_BINDING = new BlobTypeDescriptor() {
		@Override
		public JavaTypeDescriptor getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
			return typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( Blob.class );
		}

		@Override
		public <X> BasicBinder<X> getBlobBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
			return new BasicBinder<X>( javaTypeDescriptor, this ) {
				@Override
				protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
						throws SQLException {
					st.setBlob( index, javaTypeDescriptor.unwrap( value, Blob.class, options ) );
				}

				@Override
				protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
						throws SQLException {
					st.setBlob( name, javaTypeDescriptor.unwrap( value, Blob.class, options ) );
				}
			};
		}
	};

	public static final BlobTypeDescriptor STREAM_BINDING = new BlobTypeDescriptor() {
		@Override
		public JavaTypeDescriptor getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
			return null;
		}

		@Override
		public <X> BasicBinder<X> getBlobBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
			return new BasicBinder<X>( javaTypeDescriptor, this ) {
				@Override
				protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
						throws SQLException {
					final BinaryStream binaryStream = javaTypeDescriptor.unwrap(
							value,
							BinaryStream.class,
							options
					);
					st.setBinaryStream( index, binaryStream.getInputStream(), binaryStream.getLength() );
				}

				@Override
				protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
						throws SQLException {
					final BinaryStream binaryStream = javaTypeDescriptor.unwrap(
							value,
							BinaryStream.class,
							options
					);
					st.setBinaryStream( name, binaryStream.getInputStream(), binaryStream.getLength() );
				}
			};
		}
	};
}
