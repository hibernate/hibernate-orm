/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.sql.spi;

import java.sql.CallableStatement;
import java.sql.Clob;
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
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for {@link Types#CLOB CLOB} handling.
 *
 * @author Steve Ebersole
 * @author Gail Badner
 */
public abstract class ClobSqlDescriptor extends AbstractTemplateSqlTypeDescriptor {
	@Override
	public int getJdbcTypeCode() {
		return Types.CLOB;
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
	protected <X> JdbcValueExtractor<X> createExtractor(BasicJavaDescriptor<X> javaTypeDescriptor, TypeConfiguration typeConfiguration) {
		return new AbstractJdbcValueExtractor<X>( javaTypeDescriptor, this ) {
			@Override
			protected X doExtract(ResultSet rs, int position, ExecutionContext executionContext) throws SQLException {
				return javaTypeDescriptor.wrap( rs.getClob( position ), executionContext.getSession() );
			}

			@Override
			protected X doExtract(
					CallableStatement statement,
					int position,
					ExecutionContext executionContext) throws SQLException {
				return javaTypeDescriptor.wrap( statement.getClob( position ), executionContext.getSession() );
			}

			@Override
			protected X doExtract(
					CallableStatement statement,
					String name,
					ExecutionContext executionContext) throws SQLException {
				return javaTypeDescriptor.wrap( statement.getClob( name ), executionContext.getSession() );
			}
		};
	}

	@Override
	protected <X> JdbcValueBinder<X> createBinder(BasicJavaDescriptor<X> javaTypeDescriptor, TypeConfiguration typeConfiguration) {
		return getClobBinder( javaTypeDescriptor );
	}

	protected abstract <X> JdbcValueBinder<X> getClobBinder(JavaTypeDescriptor<X> javaTypeDescriptor);


	public static final ClobSqlDescriptor DEFAULT = new ClobSqlDescriptor() {
		@Override
		public <T> BasicJavaDescriptor<T> getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
			return (BasicJavaDescriptor<T>) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( Clob.class );
		}



		@Override
		public <X> JdbcValueBinder<X> getClobBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
			return new AbstractJdbcValueBinder<X>( javaTypeDescriptor, this ) {
				@Override
				protected void doBind(
						PreparedStatement st,
						int index, X value,
						ExecutionContext executionContext) throws SQLException {
					if ( executionContext.getSession().useStreamForLobBinding() ) {
						STREAM_BINDING.getClobBinder( javaTypeDescriptor )
								.bind( st, index, value, executionContext );
					}
					else {
						CLOB_BINDING.getClobBinder( javaTypeDescriptor )
								.bind( st, index, value, executionContext );
					}
				}

				@Override
				protected void doBind(
						CallableStatement st,
						String name, X value,
						ExecutionContext executionContext) throws SQLException {
					if ( executionContext.getSession().useStreamForLobBinding() ) {
						STREAM_BINDING.getClobBinder( javaTypeDescriptor )
								.bind( st, name, value, executionContext );
					}
					else {
						CLOB_BINDING.getClobBinder( javaTypeDescriptor )
								.bind( st, name, value, executionContext );
					}
				}
			};
		}
	};

	public static final ClobSqlDescriptor CLOB_BINDING = new ClobSqlDescriptor() {
		@Override
		public <T> BasicJavaDescriptor<T> getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
			return (BasicJavaDescriptor<T>) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( Clob.class );
		}

		@Override
		public <X> JdbcValueBinder<X> getClobBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
			return new AbstractJdbcValueBinder<X>( javaTypeDescriptor, this ) {
				@Override
				protected void doBind(
						PreparedStatement st,
						int index, X value,
						ExecutionContext executionContext) throws SQLException {
					st.setClob( index, javaTypeDescriptor.unwrap( value, Clob.class, executionContext.getSession() ) );
				}

				@Override
				protected void doBind(
						CallableStatement st,
						String name, X value,
						ExecutionContext executionContext) throws SQLException {
					st.setClob( name, javaTypeDescriptor.unwrap( value, Clob.class, executionContext.getSession() ) );
				}
			};
		}
	};

	public static final ClobSqlDescriptor STREAM_BINDING = new ClobSqlDescriptor() {
		@Override
		public <T> BasicJavaDescriptor<T> getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
			return (BasicJavaDescriptor<T>) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( String.class );
		}

		@Override
		public <X> JdbcValueBinder<X> getClobBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
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

	public static final ClobSqlDescriptor STREAM_BINDING_EXTRACTING = new ClobSqlDescriptor() {
		@Override
		public <T> BasicJavaDescriptor<T> getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
			return (BasicJavaDescriptor<T>) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( String.class );
		}

		@Override
		public <X> JdbcValueBinder<X> getClobBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
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

		@Override
		protected <X> JdbcValueExtractor<X> createExtractor(
				BasicJavaDescriptor<X> javaTypeDescriptor, TypeConfiguration typeConfiguration) {
			return new AbstractJdbcValueExtractor<X>( javaTypeDescriptor, this ) {
				@Override
				protected X doExtract(
						ResultSet rs,
						int position,
						ExecutionContext executionContext) throws SQLException {
					return javaTypeDescriptor.wrap( rs.getCharacterStream( position ), executionContext.getSession() );
				}

				@Override
				protected X doExtract(
						CallableStatement statement,
						int position,
						ExecutionContext executionContext) throws SQLException {
					return javaTypeDescriptor.wrap( statement.getCharacterStream( position ), executionContext.getSession() );
				}

				@Override
				protected X doExtract(
						CallableStatement statement,
						String name,
						ExecutionContext executionContext) throws SQLException {
					return javaTypeDescriptor.wrap( statement.getCharacterStream( name ), executionContext.getSession() );
				}
			};
		}
	};

}
