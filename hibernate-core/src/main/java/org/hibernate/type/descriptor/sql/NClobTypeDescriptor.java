/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
import java.sql.NClob;
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
 * Descriptor for {@link Types#NCLOB NCLOB} handling.
 *
 * @author Steve Ebersole
 * @author Gail Badner
 */
public abstract class NClobTypeDescriptor implements SqlTypeDescriptor {
	@Override
	public int getSqlType() {
		return Types.NCLOB;
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
                return javaTypeDescriptor.wrap( rs.getNClob( name ), options );
            }

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options)
					throws SQLException {
				return javaTypeDescriptor.wrap( statement.getNClob( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				return javaTypeDescriptor.wrap( statement.getNClob( name ), options );
			}
		};
	}

	protected abstract <X> BasicBinder<X> getNClobBinder(JavaTypeDescriptor<X> javaTypeDescriptor);

	@Override
    public <X> ValueBinder<X> getBinder(JavaTypeDescriptor<X> javaTypeDescriptor) {
		return getNClobBinder( javaTypeDescriptor );
	}


	public static final NClobTypeDescriptor DEFAULT =
			new NClobTypeDescriptor() {
				{
					SqlTypeDescriptorRegistry.INSTANCE.addDescriptor( this );
				}

				@Override
                public <X> BasicBinder<X> getNClobBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
					return new BasicBinder<X>( javaTypeDescriptor, this ) {
						@Override
						protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
							if ( options.useStreamForLobBinding() ) {
								STREAM_BINDING.getNClobBinder( javaTypeDescriptor ).doBind( st, value, index, options );
							}
							else {
								NCLOB_BINDING.getNClobBinder( javaTypeDescriptor ).doBind( st, value, index, options );
							}
						}
					};
				}
			};

	public static final NClobTypeDescriptor NCLOB_BINDING =
			new NClobTypeDescriptor() {
				@Override
                public <X> BasicBinder<X> getNClobBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
					return new BasicBinder<X>( javaTypeDescriptor, this ) {
						@Override
						protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
								throws SQLException {
							st.setNClob( index, javaTypeDescriptor.unwrap( value, NClob.class, options ) );
						}
					};
				}
			};

	public static final NClobTypeDescriptor STREAM_BINDING =
			new NClobTypeDescriptor() {
				@Override
                public <X> BasicBinder<X> getNClobBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
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
}
