/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.engine.jdbc.CharacterStream;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Descriptor for {@link Types#CLOB CLOB} handling.
 *
 * @author Steve Ebersole
 * @author Gail Badner
 */
public abstract class ClobJdbcType implements AdjustableJdbcType {
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
	public JdbcType resolveIndicatedType(
			JdbcTypeIndicators indicators,
			JavaType<?> domainJtd) {
		final int jdbcTypeCode = indicators.resolveJdbcTypeCode( indicators.isNationalized() ? Types.NCLOB : Types.CLOB );
		return indicators.getTypeConfiguration().getJdbcTypeRegistry().getDescriptor( jdbcTypeCode );
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return javaType.wrap( rs.getClob( paramIndex ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options)
					throws SQLException {
				return javaType.wrap( statement.getClob( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				return javaType.wrap( statement.getClob( name ), options );
			}
		};
	}

	protected abstract <X> BasicBinder<X> getClobBinder(JavaType<X> javaType);

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return getClobBinder( javaType );
	}


	public static final ClobJdbcType DEFAULT = new ClobJdbcType() {
		@Override
		public String toString() {
			return "ClobTypeDescriptor(DEFAULT)";
		}

		@Override
		public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
			return String.class;
		}

		private ClobJdbcType getDescriptor(Object value, WrapperOptions options) {
			if ( value instanceof String ) {
				// performance shortcut for binding CLOB data in String format
				return STRING_BINDING;
			}
			else if ( options.useStreamForLobBinding() ) {
				return STREAM_BINDING;
			}
			else {
				return CLOB_BINDING;
			}
		}

		@Override
		public <X> BasicBinder<X> getClobBinder(final JavaType<X> javaType) {
			return new BasicBinder<>( javaType, this ) {
				@Override
				protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
						throws SQLException {
					getDescriptor( value, options ).getClobBinder( javaType ).doBind( st, value, index, options );
				}

				@Override
				protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
						throws SQLException {
					getDescriptor( value, options ).getClobBinder( javaType ).doBind( st, value, name, options );
				}
			};
		}
	};

	public static final ClobJdbcType STRING_BINDING = new ClobJdbcType() {
		@Override
		public String toString() {
			return "ClobTypeDescriptor(STRING_BINDING)";
		}

		@Override
		public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
			return String.class;
		}

		@Override
		public <X> BasicBinder<X> getClobBinder(final JavaType<X> javaType) {
			return new BasicBinder<>( javaType, this ) {
				@Override
				protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
						throws SQLException {
					st.setString( index, javaType.unwrap( value, String.class, options ) );
				}

				@Override
				protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
						throws SQLException {
					st.setString( name, javaType.unwrap( value, String.class, options ) );
				}
			};
		}

		@Override
		public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaType) {
			return new BasicExtractor<>( javaType, this ) {
				@Override
				protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
					return javaType.wrap( rs.getString( paramIndex ), options );
				}

				@Override
				protected X doExtract(CallableStatement statement, int index, WrapperOptions options)
						throws SQLException {
					return javaType.wrap( statement.getString( index ), options );
				}

				@Override
				protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
						throws SQLException {
					return javaType.wrap( statement.getString( name ), options );
				}
			};
		}
	};

	public static final ClobJdbcType CLOB_BINDING = new ClobJdbcType() {
		@Override
		public String toString() {
			return "ClobTypeDescriptor(CLOB_BINDING)";
		}

		@Override
		public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
			return Clob.class;
		}

		@Override
		public <X> BasicBinder<X> getClobBinder(final JavaType<X> javaType) {
			return new BasicBinder<>( javaType, this ) {
				@Override
				protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
						throws SQLException {
					st.setClob( index, javaType.unwrap( value, Clob.class, options ) );
				}

				@Override
				protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
						throws SQLException {
					st.setClob( name, javaType.unwrap( value, Clob.class, options ) );
				}
			};
		}
	};

	public static final ClobJdbcType STREAM_BINDING = new ClobJdbcType() {
		@Override
		public String toString() {
			return "ClobTypeDescriptor(STREAM_BINDING)";
		}

		@Override
		public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
			return CharacterStream.class;
		}

		@Override
		public <X> BasicBinder<X> getClobBinder(final JavaType<X> javaType) {
			return new BasicBinder<>( javaType, this ) {
				@Override
				protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
						throws SQLException {
					final CharacterStream characterStream = javaType.unwrap( value, CharacterStream.class, options );
					st.setCharacterStream( index, characterStream.asReader(), characterStream.getLength() );
				}

				@Override
				protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
						throws SQLException {
					final CharacterStream characterStream = javaType.unwrap(
							value,
							CharacterStream.class,
							options
					);
					st.setCharacterStream( name, characterStream.asReader(), characterStream.getLength() );
				}
			};
		}
	};

	public static final ClobJdbcType STREAM_BINDING_EXTRACTING = new ClobJdbcType() {
		@Override
		public String toString() {
			return "ClobTypeDescriptor(STREAM_BINDING_EXTRACTING)";
		}

		@Override
		public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
			return CharacterStream.class;
		}

		@Override
		public <X> BasicBinder<X> getClobBinder(final JavaType<X> javaType) {
			return new BasicBinder<>( javaType, this ) {
				@Override
				protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
						throws SQLException {
					final CharacterStream characterStream = javaType.unwrap( value, CharacterStream.class, options );
					st.setCharacterStream( index, characterStream.asReader(), characterStream.getLength() );
				}

				@Override
				protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
						throws SQLException {
					final CharacterStream characterStream = javaType.unwrap( value, CharacterStream.class, options );
					st.setCharacterStream( name, characterStream.asReader(), characterStream.getLength() );
				}
			};
		}

		@Override
		public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaType) {
			return new BasicExtractor<>( javaType, this ) {
				@Override
				protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
					return javaType.wrap( rs.getCharacterStream( paramIndex ), options );
				}

				@Override
				protected X doExtract(CallableStatement statement, int index, WrapperOptions options)
						throws SQLException {
					return javaType.wrap( statement.getCharacterStream( index ), options );
				}

				@Override
				protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
						throws SQLException {
					return javaType.wrap( statement.getCharacterStream( name ), options );
				}
			};
		}
	};

	public static final ClobJdbcType MATERIALIZED = new ClobJdbcType() {
		@Override
		public String toString() {
			return "ClobTypeDescriptor(MATERIALIZED)";
		}

		@Override
		public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
			return String.class;
		}

		@Override
		public <X> BasicBinder<X> getClobBinder(final JavaType<X> javaType) {
			return new BasicBinder<>( javaType, this ) {
				@Override
				protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
						throws SQLException {
					st.setString( index, javaType.unwrap( value, String.class, options ) );
				}

				@Override
				protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
						throws SQLException {
					st.setString( name, javaType.unwrap( value, String.class, options ) );
				}
			};
		}

		@Override
		public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaType) {
			return new BasicExtractor<>( javaType, this ) {
				@Override
				protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
					return javaType.wrap( rs.getString( paramIndex ), options );
				}

				@Override
				protected X doExtract(CallableStatement statement, int index, WrapperOptions options)
						throws SQLException {
					return javaType.wrap( statement.getString( index ), options );
				}

				@Override
				protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
						throws SQLException {
					return javaType.wrap( statement.getString( name ), options );
				}
			};
		}
	};

}
