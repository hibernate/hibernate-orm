/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.sql;

import java.io.Serializable;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.type.descriptor.JdbcTypeNameMapper;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

import org.jboss.logging.Logger;

/**
 * Basically a map from JDBC type code (int) -> {@link SqlTypeDescriptor}
 *
 * @author Steve Ebersole
 */
public class SqlTypeDescriptorRegistry {
	public static final SqlTypeDescriptorRegistry INSTANCE = new SqlTypeDescriptorRegistry();

	private static final Logger log = Logger.getLogger( SqlTypeDescriptorRegistry.class );

	private ConcurrentHashMap<Integer,SqlTypeDescriptor> descriptorMap = new ConcurrentHashMap<Integer, SqlTypeDescriptor>();

	private SqlTypeDescriptorRegistry() {
		addDescriptor( BooleanTypeDescriptor.INSTANCE );

		addDescriptor( BitTypeDescriptor.INSTANCE );
		addDescriptor( BigIntTypeDescriptor.INSTANCE );
		addDescriptor( DecimalTypeDescriptor.INSTANCE );
		addDescriptor( DoubleTypeDescriptor.INSTANCE );
		addDescriptor( FloatTypeDescriptor.INSTANCE );
		addDescriptor( IntegerTypeDescriptor.INSTANCE );
		addDescriptor( NumericTypeDescriptor.INSTANCE );
		addDescriptor( RealTypeDescriptor.INSTANCE );
		addDescriptor( SmallIntTypeDescriptor.INSTANCE );
		addDescriptor( TinyIntTypeDescriptor.INSTANCE );

		addDescriptor( DateTypeDescriptor.INSTANCE );
		addDescriptor( TimestampTypeDescriptor.INSTANCE );
		addDescriptor( TimeTypeDescriptor.INSTANCE );

		addDescriptor( BinaryTypeDescriptor.INSTANCE );
		addDescriptor( VarbinaryTypeDescriptor.INSTANCE );
		addDescriptor( LongVarbinaryTypeDescriptor.INSTANCE );
		addDescriptor( BlobTypeDescriptor.DEFAULT );

		addDescriptor( CharTypeDescriptor.INSTANCE );
		addDescriptor( VarcharTypeDescriptor.INSTANCE );
		addDescriptor( LongVarcharTypeDescriptor.INSTANCE );
		addDescriptor( ClobTypeDescriptor.DEFAULT );

		addDescriptor( NCharTypeDescriptor.INSTANCE );
		addDescriptor( NVarcharTypeDescriptor.INSTANCE );
		addDescriptor( LongNVarcharTypeDescriptor.INSTANCE );
		addDescriptor( NClobTypeDescriptor.DEFAULT );
	}

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
		public boolean canBeRemapped() {
			return true;
		}

		@Override
		public <X> ValueBinder<X> getBinder(JavaTypeDescriptor<X> javaTypeDescriptor) {
			if ( Serializable.class.isAssignableFrom( javaTypeDescriptor.getJavaTypeClass() ) ) {
				return VarbinaryTypeDescriptor.INSTANCE.getBinder( javaTypeDescriptor );
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
			if ( Serializable.class.isAssignableFrom( javaTypeDescriptor.getJavaTypeClass() ) ) {
				return VarbinaryTypeDescriptor.INSTANCE.getExtractor( javaTypeDescriptor );
			}

			return new BasicExtractor( javaTypeDescriptor, this ) {
				@Override
				protected Object doExtract(ResultSet rs, String name, WrapperOptions options) throws SQLException {
					return rs.getObject( name );
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
