/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.vector;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.BasicArrayType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;

public class OracleVectorTypeContributor implements TypeContributor {

	@Override
	public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		final Dialect dialect = serviceRegistry.requireService( JdbcServices.class ).getDialect();

		if ( dialect instanceof OracleDialect && dialect.getVersion().isSameOrAfter( 23, 4 ) ) {
			final TypeConfiguration typeConfiguration = typeContributions.getTypeConfiguration();
			final JavaTypeRegistry javaTypeRegistry = typeConfiguration.getJavaTypeRegistry();
			final JdbcTypeRegistry jdbcTypeRegistry = typeConfiguration.getJdbcTypeRegistry();
			final BasicTypeRegistry basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();

			final boolean isVectorSupported = isVectorSupportedByDriver( (OracleDialect) dialect );

			// Register generic vector type
			final OracleVectorJdbcType genericVectorJdbcType = new OracleVectorJdbcType(
					jdbcTypeRegistry.getDescriptor( SqlTypes.FLOAT ),
					isVectorSupported
			);
			jdbcTypeRegistry.addDescriptor( SqlTypes.VECTOR, genericVectorJdbcType );
			final JdbcType floatVectorJdbcType = new OracleFloatVectorJdbcType(
					jdbcTypeRegistry.getDescriptor( SqlTypes.FLOAT ),
					isVectorSupported
			);
			jdbcTypeRegistry.addDescriptor( SqlTypes.VECTOR_FLOAT32, floatVectorJdbcType );
			final JdbcType doubleVectorJdbcType = new OracleDoubleVectorJdbcType(
					jdbcTypeRegistry.getDescriptor( SqlTypes.DOUBLE ),
					isVectorSupported
			);
			jdbcTypeRegistry.addDescriptor( SqlTypes.VECTOR_FLOAT64, doubleVectorJdbcType );
			final JdbcType byteVectorJdbcType = new OracleByteVectorJdbcType(
					jdbcTypeRegistry.getDescriptor( SqlTypes.TINYINT ),
					isVectorSupported
			);
			jdbcTypeRegistry.addDescriptor( SqlTypes.VECTOR_INT8, byteVectorJdbcType );


			// Resolving basic types  after jdbc types are registered.
			basicTypeRegistry.register(
					new BasicArrayType<>(
							basicTypeRegistry.resolve( StandardBasicTypes.FLOAT ),
							genericVectorJdbcType,
							javaTypeRegistry.getDescriptor( float[].class )
					),
					StandardBasicTypes.VECTOR.getName()
			);
			basicTypeRegistry.register(
					new BasicArrayType<>(
							basicTypeRegistry.resolve( StandardBasicTypes.FLOAT ),
							floatVectorJdbcType,
							javaTypeRegistry.getDescriptor( float[].class )
					),
					StandardBasicTypes.VECTOR_FLOAT32.getName()
			);
			basicTypeRegistry.register(
					new BasicArrayType<>(
							basicTypeRegistry.resolve( StandardBasicTypes.DOUBLE ),
							doubleVectorJdbcType,
							javaTypeRegistry.getDescriptor( double[].class )
					),
					StandardBasicTypes.VECTOR_FLOAT64.getName()
			);
			basicTypeRegistry.register(
					new BasicArrayType<>(
							basicTypeRegistry.resolve( StandardBasicTypes.BYTE ),
							byteVectorJdbcType,
							javaTypeRegistry.getDescriptor( byte[].class )
					),
					StandardBasicTypes.VECTOR_INT8.getName()
			);

			typeConfiguration.getDdlTypeRegistry().addDescriptor(
					new DdlTypeImpl( SqlTypes.VECTOR, "vector($l, *)", "vector", dialect ) {
						@Override
						public String getTypeName(Size size) {
							return OracleVectorTypeContributor.replace(
									"vector($l, *)",
									size.getArrayLength() == null ? null : size.getArrayLength().longValue()
							);
						}
					}
			);
			typeConfiguration.getDdlTypeRegistry().addDescriptor(
					new DdlTypeImpl( SqlTypes.VECTOR_INT8, "vector($l, INT8)", "vector", dialect ) {
						@Override
						public String getTypeName(Size size) {
							return OracleVectorTypeContributor.replace(
									"vector($l, INT8)",
									size.getArrayLength() == null ? null : size.getArrayLength().longValue()
							);
						}
					}
			);
			typeConfiguration.getDdlTypeRegistry().addDescriptor(
					new DdlTypeImpl( SqlTypes.VECTOR_FLOAT32, "vector($l, FLOAT32)", "vector", dialect ) {
						@Override
						public String getTypeName(Size size) {
							return OracleVectorTypeContributor.replace(
									"vector($l, FLOAT32)",
									size.getArrayLength() == null ? null : size.getArrayLength().longValue()
							);
						}
					}
			);
			typeConfiguration.getDdlTypeRegistry().addDescriptor(
					new DdlTypeImpl( SqlTypes.VECTOR_FLOAT64, "vector($l, FLOAT64)", "vector", dialect ) {
						@Override
						public String getTypeName(Size size) {
							return OracleVectorTypeContributor.replace(
									"vector($l, FLOAT64)",
									size.getArrayLength() == null ? null : size.getArrayLength().longValue()
							);
						}
					}
			);
		}
	}


	/**
	 * Replace vector dimension with the length or * for undefined length
	 */
	private static String replace(String type, Long size) {
		return StringHelper.replaceOnce( type, "$l", size != null ? size.toString() : "*" );
	}

	private boolean isVectorSupportedByDriver(OracleDialect dialect) {
		int majorVersion = dialect.getDriverMajorVersion();
		int minorVersion = dialect.getDriverMinorVersion();

		return ( majorVersion > 23 ) || ( majorVersion == 23 && minorVersion >= 4 );
	}
}
