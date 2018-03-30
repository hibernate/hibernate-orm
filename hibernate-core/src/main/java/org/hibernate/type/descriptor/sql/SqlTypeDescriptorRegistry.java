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
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

/**
 * Basically a map from JDBC type code (int) -> {@link SqlTypeDescriptor}
 *
 * @author Steve Ebersole
 *
 * @deprecated (5.3) Use {@link org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptorRegistry} instead.
 */
@Deprecated
public class SqlTypeDescriptorRegistry implements Serializable {

	/**
	 * @deprecated (5.3) Use {@link TypeConfiguration#getSqlTypeDescriptorRegistry()} instead.
	 */
	@Deprecated
	public static final SqlTypeDescriptorRegistry INSTANCE = new SqlTypeDescriptorRegistry();

	private static final Logger log = Logger.getLogger( SqlTypeDescriptorRegistry.class );

	private ConcurrentHashMap<Integer,SqlTypeDescriptor> descriptorMap = new ConcurrentHashMap<Integer, SqlTypeDescriptor>();

	protected SqlTypeDescriptorRegistry() {
		addDescriptorInternal( BooleanTypeDescriptor.INSTANCE );

		addDescriptorInternal( BitTypeDescriptor.INSTANCE );
		addDescriptorInternal( BigIntTypeDescriptor.INSTANCE );
		addDescriptorInternal( DecimalTypeDescriptor.INSTANCE );
		addDescriptorInternal( DoubleTypeDescriptor.INSTANCE );
		addDescriptorInternal( FloatTypeDescriptor.INSTANCE );
		addDescriptorInternal( IntegerTypeDescriptor.INSTANCE );
		addDescriptorInternal( NumericTypeDescriptor.INSTANCE );
		addDescriptorInternal( RealTypeDescriptor.INSTANCE );
		addDescriptorInternal( SmallIntTypeDescriptor.INSTANCE );
		addDescriptorInternal( TinyIntTypeDescriptor.INSTANCE );

		addDescriptorInternal( DateTypeDescriptor.INSTANCE );
		addDescriptorInternal( TimestampTypeDescriptor.INSTANCE );
		addDescriptorInternal( TimeTypeDescriptor.INSTANCE );

		addDescriptorInternal( BinaryTypeDescriptor.INSTANCE );
		addDescriptorInternal( VarbinaryTypeDescriptor.INSTANCE );
		addDescriptorInternal( LongVarbinaryTypeDescriptor.INSTANCE );
		addDescriptorInternal( BlobTypeDescriptor.DEFAULT );

		addDescriptorInternal( CharTypeDescriptor.INSTANCE );
		addDescriptorInternal( VarcharTypeDescriptor.INSTANCE );
		addDescriptorInternal( LongVarcharTypeDescriptor.INSTANCE );
		addDescriptorInternal( ClobTypeDescriptor.DEFAULT );

		addDescriptorInternal( NCharTypeDescriptor.INSTANCE );
		addDescriptorInternal( NVarcharTypeDescriptor.INSTANCE );
		addDescriptorInternal( LongNVarcharTypeDescriptor.INSTANCE );
		addDescriptorInternal( NClobTypeDescriptor.DEFAULT );
	}

	/**
	 * @deprecated (5.3) Use {@link org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptorRegistry#addDescriptor(SqlTypeDescriptor)} instead.
	 */
	@Deprecated
	public void addDescriptor(SqlTypeDescriptor sqlTypeDescriptor) {
		descriptorMap.put( sqlTypeDescriptor.getSqlType(), sqlTypeDescriptor );
	}

	private void addDescriptorInternal(SqlTypeDescriptor sqlTypeDescriptor){
		descriptorMap.put( sqlTypeDescriptor.getSqlType(), sqlTypeDescriptor );
	}

	/**
	 * @deprecated (5.3) Use {@link org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptorRegistry#getDescriptor(int)} instead.
	 */
	@Deprecated
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
			if ( Serializable.class.isAssignableFrom( javaTypeDescriptor.getJavaType() ) ) {
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
			if ( Serializable.class.isAssignableFrom( javaTypeDescriptor.getJavaType() ) ) {
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
