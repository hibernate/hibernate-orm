/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.engine.jdbc.CharacterStream;
import org.hibernate.type.BasicType;
import org.hibernate.type.CharacterArrayNClobType;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeDescriptorRegistry;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for {@link Types#CLOB CLOB} handling.
 *
 * @author Steve Ebersole
 * @author Gail Badner
 */
public abstract class ClobTypeDescriptor implements AdjustableJdbcTypeDescriptor {
	@Override
	public int getJdbcTypeCode() {
		return Types.CLOB;
	}

	@Override
	public String getFriendlyName() {
		return "CLOB";
	}

	@Override
	public String toString() {
		return "ClobTypeDescriptor";
	}

	@Override
	public boolean canBeRemapped() {
		return true;
	}

	@Override
	public JdbcTypeDescriptor resolveIndicatedType(
			JdbcTypeDescriptorIndicators indicators,
			JavaTypeDescriptor<?> domainJtd) {
		final TypeConfiguration typeConfiguration = indicators.getTypeConfiguration();
		final JdbcTypeDescriptorRegistry jdbcTypeRegistry = typeConfiguration.getJdbcTypeDescriptorRegistry();
		return indicators.isNationalized()
				? jdbcTypeRegistry.getDescriptor( Types.NCLOB )
				: jdbcTypeRegistry.getDescriptor( Types.CLOB );
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicExtractor<X>( javaTypeDescriptor, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( rs.getClob( paramIndex ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options)
					throws SQLException {
				return javaTypeDescriptor.wrap( statement.getClob( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				return javaTypeDescriptor.wrap( statement.getClob( name ), options );
			}
		};
	}

	protected abstract <X> BasicBinder<X> getClobBinder(JavaTypeDescriptor<X> javaTypeDescriptor);

	@Override
	public <X> ValueBinder<X> getBinder(JavaTypeDescriptor<X> javaTypeDescriptor) {
		return getClobBinder( javaTypeDescriptor );
	}


	public static final ClobTypeDescriptor DEFAULT = new ClobTypeDescriptor() {
		@Override
		public String toString() {
			return "ClobTypeDescriptor(DEFAULT)";
		}

		@Override
		public <X> BasicBinder<X> getClobBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
			return new BasicBinder<X>( javaTypeDescriptor, this ) {
				@Override
				protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
						throws SQLException {
					if ( options.useStreamForLobBinding() ) {
						STREAM_BINDING.getClobBinder( javaTypeDescriptor ).doBind( st, value, index, options );
					}
					else {
						CLOB_BINDING.getClobBinder( javaTypeDescriptor ).doBind( st, value, index, options );
					}
				}

				@Override
				protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
						throws SQLException {
					if ( options.useStreamForLobBinding() ) {
						STREAM_BINDING.getClobBinder( javaTypeDescriptor ).doBind( st, value, name, options );
					}
					else {
						CLOB_BINDING.getClobBinder( javaTypeDescriptor ).doBind( st, value, name, options );
					}
				}
			};
		}
	};

	public static final ClobTypeDescriptor STRING_BINDING = new ClobTypeDescriptor() {
		@Override
		public String toString() {
			return "ClobTypeDescriptor(STRING_BINDING)";
		}

		@Override
		public <X> BasicBinder<X> getClobBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
			return new BasicBinder<X>( javaTypeDescriptor, this ) {
				@Override
				protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
						throws SQLException {
					st.setString( index, javaTypeDescriptor.unwrap( value, String.class, options ) );
				}

				@Override
				protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
						throws SQLException {
					st.setString( name, javaTypeDescriptor.unwrap( value, String.class, options ) );
				}
			};
		}

		@Override
		public <X> ValueExtractor<X> getExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
			return new BasicExtractor<X>( javaTypeDescriptor, this ) {
				@Override
				protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
					return javaTypeDescriptor.wrap( rs.getString( paramIndex ), options );
				}

				@Override
				protected X doExtract(CallableStatement statement, int index, WrapperOptions options)
						throws SQLException {
					return javaTypeDescriptor.wrap( statement.getString( index ), options );
				}

				@Override
				protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
						throws SQLException {
					return javaTypeDescriptor.wrap( statement.getString( name ), options );
				}
			};
		}
	};

	public static final ClobTypeDescriptor CLOB_BINDING = new ClobTypeDescriptor() {
		@Override
		public String toString() {
			return "ClobTypeDescriptor(CLOB_BINDING)";
		}

		@Override
		public <X> BasicBinder<X> getClobBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
			return new BasicBinder<X>( javaTypeDescriptor, this ) {
				@Override
				protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
						throws SQLException {
					st.setClob( index, javaTypeDescriptor.unwrap( value, Clob.class, options ) );
				}

				@Override
				protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
						throws SQLException {
					st.setClob( name, javaTypeDescriptor.unwrap( value, Clob.class, options ) );
				}
			};
		}
	};

	public static final ClobTypeDescriptor STREAM_BINDING = new ClobTypeDescriptor() {
		@Override
		public String toString() {
			return "ClobTypeDescriptor(STREAM_BINDING)";
		}

		@Override
		public <X> BasicBinder<X> getClobBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
			return new BasicBinder<X>( javaTypeDescriptor, this ) {
				@Override
				protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
						throws SQLException {
					final CharacterStream characterStream = javaTypeDescriptor.unwrap(
							value,
							CharacterStream.class,
							options
					);
					st.setCharacterStream( index, characterStream.asReader(), characterStream.getLength() );
				}

				@Override
				protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
						throws SQLException {
					final CharacterStream characterStream = javaTypeDescriptor.unwrap(
							value,
							CharacterStream.class,
							options
					);
					st.setCharacterStream( name, characterStream.asReader(), characterStream.getLength() );
				}
			};
		}
	};

	public static final ClobTypeDescriptor STREAM_BINDING_EXTRACTING = new ClobTypeDescriptor() {
		@Override
		public String toString() {
			return "ClobTypeDescriptor(STREAM_BINDING_EXTRACTING)";
		}

		@Override
		public <X> BasicBinder<X> getClobBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
			return new BasicBinder<X>( javaTypeDescriptor, this ) {
				@Override
				protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
						throws SQLException {
					final CharacterStream characterStream = javaTypeDescriptor.unwrap(
							value,
							CharacterStream.class,
							options
					);
					st.setCharacterStream( index, characterStream.asReader(), characterStream.getLength() );
				}

				@Override
				protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
						throws SQLException {
					final CharacterStream characterStream = javaTypeDescriptor.unwrap(
							value,
							CharacterStream.class,
							options
					);
					st.setCharacterStream( name, characterStream.asReader(), characterStream.getLength() );
				}
			};
		}

		@Override
		public <X> ValueExtractor<X> getExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
			return new BasicExtractor<X>( javaTypeDescriptor, this ) {
				@Override
				protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
					return javaTypeDescriptor.wrap( rs.getCharacterStream( paramIndex ), options );
				}

				@Override
				protected X doExtract(CallableStatement statement, int index, WrapperOptions options)
						throws SQLException {
					return javaTypeDescriptor.wrap( statement.getCharacterStream( index ), options );
				}

				@Override
				protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
						throws SQLException {
					return javaTypeDescriptor.wrap( statement.getCharacterStream( name ), options );
				}
			};
		}
	};

}
