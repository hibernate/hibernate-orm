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
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Descriptor for {@link Types#CLOB CLOB} handling.
 *
 * @author Steve Ebersole
 * @author Gail Badner
 */
public abstract class ClobTypeDescriptor implements SqlTypeDescriptor {
	@Override
	public int getSqlType() {
		return Types.CLOB;
	}

	@Override
	public boolean canBeRemapped() {
		return true;
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicExtractor<X>( javaTypeDescriptor, this ) {
			@Override
            protected X doExtract(ResultSet rs, String name, WrapperOptions options) throws SQLException {
                return javaTypeDescriptor.wrap( rs.getClob( name ), options );
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


	public static final ClobTypeDescriptor DEFAULT =
			new ClobTypeDescriptor() {
				{
					SqlTypeDescriptorRegistry.INSTANCE.addDescriptor( this );
				}

				@Override
                public <X> BasicBinder<X> getClobBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
					return new BasicBinder<X>( javaTypeDescriptor, this ) {
						@Override
						protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
							if ( options.useStreamForLobBinding() ) {
								STREAM_BINDING.getClobBinder( javaTypeDescriptor ).doBind( st, value, index, options );
							}
							else {
								CLOB_BINDING.getClobBinder( javaTypeDescriptor ).doBind( st, value, index, options );
							}
						}
					};
				}
			};

	public static final ClobTypeDescriptor CLOB_BINDING =
			new ClobTypeDescriptor() {
				@Override
                public <X> BasicBinder<X> getClobBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
					return new BasicBinder<X>( javaTypeDescriptor, this ) {
						@Override
						protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
								throws SQLException {
							st.setClob( index, javaTypeDescriptor.unwrap( value, Clob.class, options ) );
						}
					};
				}
			};

	public static final ClobTypeDescriptor STREAM_BINDING =
			new ClobTypeDescriptor() {
				@Override
                public <X> BasicBinder<X> getClobBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
					return new BasicBinder<X>( javaTypeDescriptor, this ) {
						@Override
						protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
								throws SQLException {
							final CharacterStream characterStream = javaTypeDescriptor.unwrap( value, CharacterStream.class, options );
							st.setCharacterStream( index, characterStream.asReader(), characterStream.getLength() );
						}
					};
				}
			};

	public static final ClobTypeDescriptor STREAM_BINDING_EXTRACTING =
			new ClobTypeDescriptor() {
				@Override
                public <X> BasicBinder<X> getClobBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
					return new BasicBinder<X>( javaTypeDescriptor, this ) {
						@Override
						protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
								throws SQLException {
							final CharacterStream characterStream = javaTypeDescriptor.unwrap( value, CharacterStream.class, options );
							st.setCharacterStream( index, characterStream.asReader(), characterStream.getLength() );
						}
					};
				}
				
				@Override
				public <X> ValueExtractor<X> getExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
					return new BasicExtractor<X>( javaTypeDescriptor, this ) {
						@Override
			            protected X doExtract(ResultSet rs, String name, WrapperOptions options) throws SQLException {
			                return javaTypeDescriptor.wrap( rs.getCharacterStream( name ), options );
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
