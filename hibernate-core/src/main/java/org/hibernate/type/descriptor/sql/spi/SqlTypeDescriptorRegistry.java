/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.sql.spi;

import java.io.Serializable;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.spi.ValueBinder;
import org.hibernate.type.descriptor.spi.ValueExtractor;
import org.hibernate.type.descriptor.spi.WrapperOptions;
import org.hibernate.type.descriptor.sql.internal.SqlTypeDescriptorBaseline;
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

/**
 * Basically a map from JDBC type code (int) -> {@link SqlTypeDescriptor}
 *
 * @author Steve Ebersole
 */
public class SqlTypeDescriptorRegistry implements SqlTypeDescriptorBaseline.BaselineTarget {
	private static final Logger log = Logger.getLogger( SqlTypeDescriptorRegistry.class );

	private final TypeConfiguration typeConfiguration;
	private ConcurrentHashMap<Integer, SqlTypeDescriptor> descriptorMap = new ConcurrentHashMap<>();

	public SqlTypeDescriptorRegistry(TypeConfiguration typeConfiguration) {
		this.typeConfiguration = typeConfiguration;
		SqlTypeDescriptorBaseline.prime( this );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// baseline descriptors

	@Override
	public void addDescriptor(SqlTypeDescriptor sqlTypeDescriptor) {
		descriptorMap.put( sqlTypeDescriptor.getSqlType(), sqlTypeDescriptor );
	}

	public SqlTypeDescriptor getDescriptor(int jdbcTypeCode) {
		SqlTypeDescriptor descriptor = descriptorMap.get( Integer.valueOf( jdbcTypeCode ) );
		if ( descriptor != null ) {
			return descriptor;
		}

		if ( JdbcTypeNameMapper.isStandardTypeCode( jdbcTypeCode ) ) {
			log.debugf(
					"A standard JDBC type code [%s] was not defined in SqlTypeDescriptorRegistry",
					jdbcTypeCode
			);
		}

		// see if the typecode is part of a known type family...
		JdbcTypeFamilyInformation.Family family = JdbcTypeFamilyInformation.INSTANCE.locateJdbcTypeFamilyByTypeCode( jdbcTypeCode );
		if ( family != null ) {
			for ( int potentialAlternateTypeCode : family.getTypeCodes() ) {
				if ( potentialAlternateTypeCode != jdbcTypeCode ) {
					final SqlTypeDescriptor potentialAlternateDescriptor = descriptorMap.get( Integer.valueOf( potentialAlternateTypeCode ) );
					if ( potentialAlternateDescriptor != null ) {
						// todo : add a SqlTypeDescriptor.canBeAssignedFrom method...
						return potentialAlternateDescriptor;
					}

					if ( JdbcTypeNameMapper.isStandardTypeCode( potentialAlternateTypeCode ) ) {
						log.debugf(
								"A standard JDBC type code [%s] was not defined in SqlTypeDescriptorRegistry",
								potentialAlternateTypeCode
						);
					}
				}
			}
		}

		// finally, create a new descriptor mapping to getObject/setObject for this type code...
		final ObjectSqlTypeDescriptor fallBackDescriptor = new ObjectSqlTypeDescriptor( jdbcTypeCode );
		addDescriptor( fallBackDescriptor );
		return fallBackDescriptor;
	}

	public static class ObjectSqlTypeDescriptor implements SqlTypeDescriptor {
		private final int jdbcTypeCode;

		public ObjectSqlTypeDescriptor(int jdbcTypeCode) {
			this.jdbcTypeCode = jdbcTypeCode;
		}

		@Override
		public int getSqlType() {
			return jdbcTypeCode;
		}

		@Override
		public <T> BasicJavaDescriptor<T> getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
			throw new UnsupportedOperationException( "No recommended Java-type mapping known for JDBC type code [" + jdbcTypeCode + "]" );
		}

		@Override
		public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaTypeDescriptor<T> javaTypeDescriptor) {
			// obviously no literal support here :)
			return null;
		}

		@Override
		public boolean canBeRemapped() {
			return true;
		}

		@Override
		public <X> ValueBinder<X> getBinder(JavaTypeDescriptor<X> javaTypeDescriptor) {
			if ( Serializable.class.isAssignableFrom( javaTypeDescriptor.getJavaType() ) ) {
				return VarbinarySqlDescriptor.INSTANCE.getBinder( javaTypeDescriptor );
			}

			return new BasicBinder<X>( javaTypeDescriptor, this ) {
				@Override
				protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
						throws SQLException {
					st.setObject( index, value, jdbcTypeCode );
				}

				@Override
				protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
						throws SQLException {
					st.setObject( name, value, jdbcTypeCode );
				}
			};
		}

		@Override
		@SuppressWarnings("unchecked")
		public ValueExtractor getExtractor(JavaTypeDescriptor javaTypeDescriptor) {
			if ( Serializable.class.isAssignableFrom( javaTypeDescriptor.getJavaType() ) ) {
				return VarbinarySqlDescriptor.INSTANCE.getExtractor( javaTypeDescriptor );
			}

			return new BasicExtractor( javaTypeDescriptor, this ) {
				@Override
				protected Object doExtract(ResultSet rs, int position, WrapperOptions options) throws SQLException {
					return rs.getObject( position );
				}

				@Override
				protected Object doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
					return statement.getObject( index );
				}

				@Override
				protected Object doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
					return statement.getObject( name );
				}
			};
		}
	}
}
