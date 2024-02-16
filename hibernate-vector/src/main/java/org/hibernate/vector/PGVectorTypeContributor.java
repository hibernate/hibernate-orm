/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.vector;

import java.lang.reflect.Type;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.BasicArrayType;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;

public class PGVectorTypeContributor implements TypeContributor {

	private static final Type[] VECTOR_JAVA_TYPES = {
			Float[].class,
			float[].class
	};

	@Override
	public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		final Dialect dialect = serviceRegistry.requireService( JdbcServices.class ).getDialect();
		if ( dialect instanceof PostgreSQLDialect ) {
			final TypeConfiguration typeConfiguration = typeContributions.getTypeConfiguration();
			final JavaTypeRegistry javaTypeRegistry = typeConfiguration.getJavaTypeRegistry();
			final JdbcTypeRegistry jdbcTypeRegistry = typeConfiguration.getJdbcTypeRegistry();
			final BasicTypeRegistry basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();
			final BasicType<Float> floatBasicType = basicTypeRegistry.resolve( StandardBasicTypes.FLOAT );
			final ArrayJdbcType vectorJdbcType = new VectorJdbcType( jdbcTypeRegistry.getDescriptor( SqlTypes.FLOAT ) );
			jdbcTypeRegistry.addDescriptor( SqlTypes.VECTOR, vectorJdbcType );
			for ( Type vectorJavaType : VECTOR_JAVA_TYPES ) {
				basicTypeRegistry.register(
						new BasicArrayType<>(
								floatBasicType,
								vectorJdbcType,
								javaTypeRegistry.getDescriptor( vectorJavaType )
						),
						StandardBasicTypes.VECTOR.getName()
				);
			}
			typeConfiguration.getDdlTypeRegistry().addDescriptor(
					new DdlTypeImpl( SqlTypes.VECTOR, "vector($l)", "vector", dialect ) {
						@Override
						public String getTypeName(Size size) {
							return getTypeName(
									size.getArrayLength() == null ? null : size.getArrayLength().longValue(),
									null,
									null
							);
						}
					}
			);
		}
	}
}
