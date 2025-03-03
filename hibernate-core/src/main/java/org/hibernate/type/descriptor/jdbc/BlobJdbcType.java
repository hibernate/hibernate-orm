/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.engine.jdbc.BinaryStream;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for {@link Types#BLOB BLOB} handling.
 *
 * @author Steve Ebersole
 * @author Gail Badner
 * @author Brett Meyer
 */
public abstract class BlobJdbcType implements JdbcType {

	private BlobJdbcType() {
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.BLOB;
	}

	@Override
	public String getFriendlyName() {
		return "BLOB";
	}

	@Override
	public String toString() {
		return "BlobTypeDescriptor";
	}

	@Override
	public <T> JavaType<T> getJdbcRecommendedJavaTypeMapping(
			Integer length,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		return typeConfiguration.getJavaTypeRegistry().getDescriptor( Blob.class );
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return javaType.wrap( rs.getBlob( paramIndex ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return javaType.wrap( statement.getBlob( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				return javaType.wrap( statement.getBlob( name ), options );
			}
		};
	}

	protected abstract <X> BasicBinder<X> getBlobBinder(final JavaType<X> javaType);

	@Override
	public <X> BasicBinder<X> getBinder(final JavaType<X> javaType) {
		return getBlobBinder( javaType );
	}

	public static final BlobJdbcType DEFAULT = new BlobJdbcType() {
		@Override
		public String toString() {
			return "BlobTypeDescriptor(DEFAULT)";
		}

		@Override
		public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
			return byte[].class;
		}

		private BlobJdbcType getDescriptor(Object value, WrapperOptions options) {
			if ( value instanceof byte[] ) {
				// performance shortcut for binding BLOB data in byte[] format
				return PRIMITIVE_ARRAY_BINDING;
			}
			else if ( options.useStreamForLobBinding() ) {
				return STREAM_BINDING;
			}
			else {
				return BLOB_BINDING;
			}
		}

		@Override
		public <X> BasicBinder<X> getBlobBinder(final JavaType<X> javaType) {
			return new BasicBinder<>( javaType, this ) {
				@Override
				protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
						throws SQLException {
					getDescriptor( value, options ).getBlobBinder( javaType ).doBind( st, value, index, options );
				}

				@Override
				protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
						throws SQLException {
					getDescriptor( value, options ).getBlobBinder( javaType ).doBind( st, value, name, options );
				}
			};
		}
	};

	public static final BlobJdbcType PRIMITIVE_ARRAY_BINDING = new BlobJdbcType() {
		@Override
		public String toString() {
			return "BlobTypeDescriptor(PRIMITIVE_ARRAY_BINDING)";
		}

		@Override
		public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
			return byte[].class;
		}

		@Override
		public <X> BasicBinder<X> getBlobBinder(final JavaType<X> javaType) {
			return new BasicBinder<>( javaType, this ) {
				@Override
				public void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
						throws SQLException {
					st.setBytes( index, javaType.unwrap( value, byte[].class, options ) );
				}

				@Override
				protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
						throws SQLException {
					st.setBytes( name, javaType.unwrap( value, byte[].class, options ) );
				}
			};
		}
	};

	public static final BlobJdbcType BLOB_BINDING = new BlobJdbcType() {
		@Override
		public String toString() {
			return "BlobTypeDescriptor(BLOB_BINDING)";
		}

		@Override
		public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
			return Blob.class;
		}

		@Override
		public <X> BasicBinder<X> getBlobBinder(final JavaType<X> javaType) {
			return new BasicBinder<>( javaType, this ) {
				@Override
				protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
						throws SQLException {
					st.setBlob( index, javaType.unwrap( value, Blob.class, options ) );
				}

				@Override
				protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
						throws SQLException {
					st.setBlob( name, javaType.unwrap( value, Blob.class, options ) );
				}
			};
		}
	};

	public static final BlobJdbcType STREAM_BINDING = new BlobJdbcType() {
		@Override
		public String toString() {
			return "BlobTypeDescriptor(STREAM_BINDING)";
		}

		@Override
		public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
			return BinaryStream.class;
		}

		@Override
		public <X> BasicBinder<X> getBlobBinder(final JavaType<X> javaType) {
			return new BasicBinder<>( javaType, this ) {
				@Override
				protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
						throws SQLException {
					// the use of BinaryStream here instead of InputStream seems to be only necessary on Oracle
					final BinaryStream binaryStream = javaType.unwrap( value, BinaryStream.class, options );
					st.setBinaryStream( index, binaryStream.getInputStream(), binaryStream.getLength() );
				}

				@Override
				protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
						throws SQLException {
					// the use of BinaryStream here instead of InputStream seems to be only necessary on Oracle
					final BinaryStream binaryStream = javaType.unwrap( value, BinaryStream.class, options );
					st.setBinaryStream( name, binaryStream.getInputStream(), binaryStream.getLength() );
				}
			};
		}
	};

	public static final BlobJdbcType MATERIALIZED = new BlobJdbcType() {
		@Override
		public String toString() {
			return "BlobTypeDescriptor(MATERIALIZED)";
		}

		@Override
		public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
			return byte[].class;
		}

		@Override
		public <X> BasicBinder<X> getBlobBinder(final JavaType<X> javaType) {
			return new BasicBinder<>( javaType, this ) {
				@Override
				public void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
						throws SQLException {
					st.setBytes( index, javaType.unwrap( value, byte[].class, options ) );
				}

				@Override
				protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
						throws SQLException {
					st.setBytes( name, javaType.unwrap( value, byte[].class, options ) );
				}
			};
		}

		@Override
		public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaType) {
			return new BasicExtractor<>( javaType, this ) {
				@Override
				protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
					return javaType.wrap( rs.getBytes( paramIndex ), options );
				}

				@Override
				protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
					return javaType.wrap( statement.getBytes( index ), options );
				}

				@Override
				protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
						throws SQLException {
					return javaType.wrap( statement.getBytes( name ), options );
				}
			};
		}
	};

}
