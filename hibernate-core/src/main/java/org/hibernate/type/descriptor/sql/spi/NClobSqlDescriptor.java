/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.sql.spi;

import java.sql.CallableStatement;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.engine.jdbc.CharacterStream;
import org.hibernate.sql.AbstractJdbcValueBinder;
import org.hibernate.sql.AbstractJdbcValueExtractor;
import org.hibernate.sql.JdbcValueBinder;
import org.hibernate.sql.JdbcValueExtractor;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Descriptor for {@link Types#NCLOB NCLOB} handling.
 *
 * @author Steve Ebersole
 * @author Gail Badner
 */
public abstract class NClobSqlDescriptor extends AbstractTemplateSqlTypeDescriptor {
	@Override
	public int getJdbcTypeCode() {
		return Types.NCLOB;
	}

	@Override
	public boolean canBeRemapped() {
		return true;
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaTypeDescriptor<T> javaTypeDescriptor) {
		// literal values for (N)CLOB data is not supported.
		return null;
	}

	@Override
	protected <X> JdbcValueBinder<X> createBinder(
			BasicJavaDescriptor<X> javaTypeDescriptor,
			TypeConfiguration typeConfiguration) {
		return getNClobBinder( javaTypeDescriptor, typeConfiguration );
	}

	protected abstract <X> JdbcValueBinder<X> getNClobBinder(
			JavaTypeDescriptor<X> javaTypeDescriptor,
			TypeConfiguration typeConfiguration);

	@Override
	protected <X> JdbcValueExtractor<X> createExtractor(
			BasicJavaDescriptor<X> javaTypeDescriptor,
			TypeConfiguration typeConfiguration) {
		return new AbstractJdbcValueExtractor<X>( javaTypeDescriptor, this ) {
			@Override
			protected X doExtract(
					ResultSet rs,
					int position,
					ExecutionContext executionContext) throws SQLException {
				return javaTypeDescriptor.wrap( rs.getNClob( position ), executionContext.getSession() );
			}

			@Override
			protected X doExtract(
					CallableStatement statement,
					int position,
					ExecutionContext executionContext) throws SQLException {
				return javaTypeDescriptor.wrap( statement.getNClob( position ), executionContext.getSession() );
			}

			@Override
			protected X doExtract(
					CallableStatement statement,
					String name,
					ExecutionContext executionContext) throws SQLException {
				return javaTypeDescriptor.wrap( statement.getNClob( name ), executionContext.getSession() );
			}
		};
	}


	public static final NClobSqlDescriptor DEFAULT = new NClobSqlDescriptor() {
		@Override
		public <T> BasicJavaDescriptor<T> getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
			return (BasicJavaDescriptor<T>) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( NClob.class );
		}

		@Override
		public <X> JdbcValueBinder<X> getNClobBinder(
				final JavaTypeDescriptor<X> javaTypeDescriptor,
				TypeConfiguration typeConfiguration) {
			return new AbstractJdbcValueBinder<X>( javaTypeDescriptor, this ) {
				@Override
				protected void doBind(
						PreparedStatement st,
						int index, X value,
						ExecutionContext executionContext) throws SQLException {
					if ( executionContext.getSession().useStreamForLobBinding() ) {
						STREAM_BINDING.getNClobBinder( javaTypeDescriptor, typeConfiguration )
								.bind( st, index, value, executionContext );
					}
					else {
						NCLOB_BINDING.getNClobBinder( javaTypeDescriptor, typeConfiguration )
								.bind( st, index, value, executionContext );
					}
				}


				@Override
				protected void doBind(
						CallableStatement st,
						String name, X value,
						ExecutionContext executionContext) throws SQLException {
					if ( executionContext.getSession().useStreamForLobBinding() ) {
						STREAM_BINDING.getNClobBinder( javaTypeDescriptor,typeConfiguration )
								.bind( st, name, value, executionContext );
					}
					else {
						NCLOB_BINDING.getNClobBinder( javaTypeDescriptor, typeConfiguration )
								.bind( st, name, value, executionContext );
					}
				}
			};
		}
	};

	public static final NClobSqlDescriptor NCLOB_BINDING = new NClobSqlDescriptor() {
		@Override
		public <T> BasicJavaDescriptor<T> getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
			return (BasicJavaDescriptor<T>) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( NClob.class );
		}

		@Override
		@SuppressWarnings("unchecked")
		protected JdbcValueBinder getNClobBinder(
				JavaTypeDescriptor javaTypeDescriptor,
				TypeConfiguration typeConfiguration) {
			return new AbstractJdbcValueBinder( javaTypeDescriptor, this ) {
				@Override
				protected void doBind(
						PreparedStatement st,
						int index, Object value,
						ExecutionContext executionContext) throws SQLException {
					st.setNClob( index, (NClob) javaTypeDescriptor.unwrap( value, NClob.class, executionContext.getSession() ) );
				}

				@Override
				protected void doBind(
						CallableStatement st,
						String name, Object value,
						ExecutionContext executionContext) throws SQLException {
					st.setNClob( name, (NClob) javaTypeDescriptor.unwrap( value, NClob.class, executionContext.getSession() ) );
				}
			};
		}
	};

	public static final NClobSqlDescriptor STREAM_BINDING = new NClobSqlDescriptor() {
		@Override
		public <T> BasicJavaDescriptor<T> getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
			return null;
		}

		@Override
		public <X> JdbcValueBinder<X> getNClobBinder(
				final JavaTypeDescriptor<X> javaTypeDescriptor,
				TypeConfiguration typeConfiguration) {
			return new AbstractJdbcValueBinder<X>( javaTypeDescriptor, this ) {
				@Override
				protected void doBind(
						PreparedStatement st,
						int index, X value,
						ExecutionContext executionContext) throws SQLException {
					final CharacterStream characterStream = javaTypeDescriptor.unwrap(
							value,
							CharacterStream.class,
							executionContext.getSession()
					);
					st.setCharacterStream( index, characterStream.asReader(), characterStream.getLength() );
				}

				@Override
				protected void doBind(
						CallableStatement st,
						String name, X value,
						ExecutionContext executionContext) throws SQLException {
					final CharacterStream characterStream = javaTypeDescriptor.unwrap(
							value,
							CharacterStream.class,
							executionContext.getSession()
					);
					st.setCharacterStream( name, characterStream.asReader(), characterStream.getLength() );
				}
			};
		}
	};
}
