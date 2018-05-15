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

import org.hibernate.sql.AbstractJdbcValueBinder;
import org.hibernate.sql.AbstractJdbcValueExtractor;
import org.hibernate.sql.JdbcValueBinder;
import org.hibernate.sql.JdbcValueExtractor;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.internal.SqlTypeDescriptorBaseline;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

/**
 * Basically a map from JDBC type code (int) -> {@link SqlTypeDescriptor}
 *
 * @author Steve Ebersole
 */
public class SqlTypeDescriptorRegistry implements SqlTypeDescriptorBaseline.BaselineTarget {
	private static final Logger log = Logger.getLogger( SqlTypeDescriptorRegistry.class );

	private ConcurrentHashMap<Integer, SqlTypeDescriptor> descriptorMap = new ConcurrentHashMap<>();

	public SqlTypeDescriptorRegistry(TypeConfiguration typeConfiguration) {
//		this.typeConfiguration = typeConfiguration;
		SqlTypeDescriptorBaseline.prime( this );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// baseline descriptors

	@Override
	public void addDescriptor(SqlTypeDescriptor sqlTypeDescriptor) {
		descriptorMap.put( sqlTypeDescriptor.getJdbcTypeCode(), sqlTypeDescriptor );
	}

	public SqlTypeDescriptor getDescriptor(int jdbcTypeCode) {
		SqlTypeDescriptor descriptor = descriptorMap.get( jdbcTypeCode );
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
					final SqlTypeDescriptor potentialAlternateDescriptor = descriptorMap.get( potentialAlternateTypeCode );
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

	public static class ObjectSqlTypeDescriptor extends AbstractTemplateSqlTypeDescriptor {
		private final int jdbcTypeCode;

		@SuppressWarnings("WeakerAccess")
		public ObjectSqlTypeDescriptor(int jdbcTypeCode) {
			this.jdbcTypeCode = jdbcTypeCode;
		}

		@Override
		public int getJdbcTypeCode() {
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
		@SuppressWarnings("unchecked")
		public <X> JdbcValueBinder<X> createBinder(BasicJavaDescriptor<X> javaTypeDescriptor, TypeConfiguration typeConfiguration) {
			if ( Serializable.class.isAssignableFrom( javaTypeDescriptor.getJavaType() ) ) {
				return VarbinarySqlDescriptor.INSTANCE.getSqlExpressableType( javaTypeDescriptor, typeConfiguration ).getJdbcValueBinder();
			}

			return new AbstractJdbcValueBinder<X>( javaTypeDescriptor, this ) {
				@Override
				protected void doBind(
						PreparedStatement st,
						int index, X value,
						ExecutionContext executionContext) throws SQLException {
					st.setObject( index, value, jdbcTypeCode );
				}

				@Override
				protected void doBind(
						CallableStatement st,
						String name,
						X value,
						ExecutionContext executionContext)
						throws SQLException {
					st.setObject( name, value, jdbcTypeCode );
				}
			};
		}


		@Override
		@SuppressWarnings("unchecked")
		public <X> JdbcValueExtractor<X> createExtractor(BasicJavaDescriptor<X> javaTypeDescriptor, TypeConfiguration typeConfiguration) {
			if ( Serializable.class.isAssignableFrom( javaTypeDescriptor.getJavaType() ) ) {
				return VarbinarySqlDescriptor.INSTANCE.getSqlExpressableType( javaTypeDescriptor, typeConfiguration ).getJdbcValueExtractor();
			}

			return new AbstractJdbcValueExtractor<X>( javaTypeDescriptor, this ) {
				@Override
				protected X doExtract(ResultSet rs, int position, ExecutionContext executionContext) throws SQLException {
					return (X) rs.getObject( position );
				}

				@Override
				protected X doExtract(CallableStatement statement, int position, ExecutionContext executionContext) throws SQLException {
					return (X) statement.getObject( position );
				}

				@Override
				protected X doExtract(CallableStatement statement, String name, ExecutionContext executionContext) throws SQLException {
					return (X) statement.getObject( name );
				}
			};
		}
	}
}
