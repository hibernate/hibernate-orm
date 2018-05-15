/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.sql.spi;

import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.engine.jdbc.BinaryStream;
import org.hibernate.sql.AbstractJdbcValueBinder;
import org.hibernate.sql.AbstractJdbcValueExtractor;
import org.hibernate.sql.JdbcValueBinder;
import org.hibernate.sql.JdbcValueExtractor;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for {@link Types#BLOB BLOB} handling.
 *
 * @author Steve Ebersole
 * @author Gail Badner
 * @author Brett Meyer
 */
public abstract class BlobSqlDescriptor extends AbstractTemplateSqlTypeDescriptor {

	private BlobSqlDescriptor() {
	}

	@Override
	public int getJdbcTypeCode() {
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
	protected <X> JdbcValueExtractor<X> createExtractor(BasicJavaDescriptor<X> javaTypeDescriptor, TypeConfiguration typeConfiguration) {
		return new AbstractJdbcValueExtractor<X>( javaTypeDescriptor, this ) {
			@Override
			protected X doExtract(
					ResultSet rs,
					int position,
					ExecutionContext executionContext) throws SQLException {
				return javaTypeDescriptor.wrap( rs.getBlob( position ), executionContext.getSession() );
			}

			@Override
			protected X doExtract(
					CallableStatement statement,
					int position,
					ExecutionContext executionContext) throws SQLException {
				return javaTypeDescriptor.wrap( statement.getBlob( position ), executionContext.getSession() );
			}

			@Override
			protected X doExtract(
					CallableStatement statement,
					String name,
					ExecutionContext executionContext) throws SQLException {
				return javaTypeDescriptor.wrap( statement.getBlob( name ), executionContext.getSession() );
			}
		};
	}

	@Override
	protected <X> JdbcValueBinder<X> createBinder(BasicJavaDescriptor<X> javaTypeDescriptor, TypeConfiguration typeConfiguration) {
		return getBlobBinder( javaTypeDescriptor, typeConfiguration );
	}

	protected abstract <X> JdbcValueBinder<X> getBlobBinder(JavaTypeDescriptor<X> javaTypeDescriptor, TypeConfiguration typeConfiguration);


	public static final BlobSqlDescriptor DEFAULT = new BlobSqlDescriptor() {
		@Override
		public <T> BasicJavaDescriptor<T> getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
			return (BasicJavaDescriptor<T>) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( Blob.class );
		}

		@Override
		public <X> JdbcValueBinder<X> getBlobBinder(JavaTypeDescriptor<X> javaTypeDescriptor, TypeConfiguration typeConfiguration) {
			return new AbstractJdbcValueBinder<X>( javaTypeDescriptor, this ) {
				@Override
				protected void doBind(
						PreparedStatement st,
						int index, X value,
						ExecutionContext executionContext) throws SQLException {
					BlobSqlDescriptor descriptor = BLOB_BINDING;
					if ( byte[].class.isInstance( value ) ) {
						// performance shortcut for binding BLOB data in byte[] format
						descriptor = PRIMITIVE_ARRAY_BINDING;
					}
					else if ( executionContext.getSession().useStreamForLobBinding() ) {
						descriptor = STREAM_BINDING;
					}

					descriptor.getBlobBinder( javaTypeDescriptor, typeConfiguration ).bind( st,
																							index,
																							value,
																							executionContext );
				}

				@Override
				protected void doBind(
						CallableStatement st,
						String name, X value,
						ExecutionContext executionContext) throws SQLException {
					BlobSqlDescriptor descriptor = BLOB_BINDING;
					if ( byte[].class.isInstance( value ) ) {
						// performance shortcut for binding BLOB data in byte[] format
						descriptor = PRIMITIVE_ARRAY_BINDING;
					}
					else if ( executionContext.getSession().useStreamForLobBinding() ) {
						descriptor = STREAM_BINDING;
					}

					descriptor.getBlobBinder( javaTypeDescriptor, typeConfiguration ).bind( st,
																							name,
																							value,
																							executionContext );
				}
			};
		}
	};

	public static final BlobSqlDescriptor PRIMITIVE_ARRAY_BINDING = new BlobSqlDescriptor() {
		@Override
		public <T> BasicJavaDescriptor<T> getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
			return (BasicJavaDescriptor<T>) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( byte[].class );
		}

		@Override
		public <X> JdbcValueBinder<X> getBlobBinder(JavaTypeDescriptor<X> javaTypeDescriptor, TypeConfiguration typeConfiguration) {
			return new AbstractJdbcValueBinder<X>( javaTypeDescriptor, this ) {
				@Override
				protected void doBind(
						PreparedStatement st,
						int index, X value,
						ExecutionContext executionContext) throws SQLException {
					st.setBytes( index, javaTypeDescriptor.unwrap( value, byte[].class, executionContext.getSession() ) );
				}

				@Override
				protected void doBind(
						CallableStatement st,
						String name, X value,
						ExecutionContext executionContext) throws SQLException {
					st.setBytes( name, javaTypeDescriptor.unwrap( value, byte[].class, executionContext.getSession() ) );
				}
			};
		}
	};

	public static final BlobSqlDescriptor BLOB_BINDING = new BlobSqlDescriptor() {
		@Override
		public <T> BasicJavaDescriptor<T> getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
			return (BasicJavaDescriptor<T>) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( Blob.class );
		}

		@Override
		public <X> JdbcValueBinder<X> getBlobBinder(JavaTypeDescriptor<X> javaTypeDescriptor, TypeConfiguration typeConfiguration) {
			return new AbstractJdbcValueBinder<X>( javaTypeDescriptor, this ) {
				@Override
				protected void doBind(
						PreparedStatement st,
						int index, X value,
						ExecutionContext executionContext) throws SQLException {
					st.setBlob( index, javaTypeDescriptor.unwrap( value, Blob.class, executionContext.getSession() ) );
				}

				@Override
				protected void doBind(
						CallableStatement st,
						String name, X value,
						ExecutionContext executionContext) throws SQLException {
					st.setBlob( name, javaTypeDescriptor.unwrap( value, Blob.class, executionContext.getSession() ) );
				}
			};
		}
	};

	public static final BlobSqlDescriptor STREAM_BINDING = new BlobSqlDescriptor() {
		@Override
		public <T> BasicJavaDescriptor<T> getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
			return null;
		}

		@Override
		public <X> JdbcValueBinder<X> getBlobBinder(JavaTypeDescriptor<X> javaTypeDescriptor, TypeConfiguration typeConfiguration) {
			return new AbstractJdbcValueBinder<X>( javaTypeDescriptor, this ) {
				@Override
				protected void doBind(
						PreparedStatement st,
						int index, X value,
						ExecutionContext executionContext) throws SQLException {
					final BinaryStream binaryStream = javaTypeDescriptor.unwrap(
							value,
							BinaryStream.class,
							executionContext.getSession()
					);
					st.setBinaryStream( index, binaryStream.getInputStream(), binaryStream.getLength() );
				}

				@Override
				protected void doBind(
						CallableStatement st,
						String name, X value,
						ExecutionContext executionContext) throws SQLException {
					final BinaryStream binaryStream = javaTypeDescriptor.unwrap(
							value,
							BinaryStream.class,
							executionContext.getSession()
					);
					st.setBinaryStream( name, binaryStream.getInputStream(), binaryStream.getLength() );
				}
			};
		}
	};
}
