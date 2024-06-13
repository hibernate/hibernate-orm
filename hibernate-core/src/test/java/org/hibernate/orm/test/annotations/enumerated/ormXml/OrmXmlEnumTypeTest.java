/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.enumerated.ormXml;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.junit4.ExtraAssertions;
import org.hibernate.type.internal.BasicTypeImpl;
import org.junit.Test;

import static java.sql.Types.VARCHAR;
import static org.hibernate.type.SqlTypes.ENUM;
import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-7645" )
public class OrmXmlEnumTypeTest extends BaseUnitTestCase {
	@Test
	public void testOrmXmlDefinedEnumType() {
		StandardServiceRegistry ssr = ServiceRegistryBuilder.buildServiceRegistry();

		try {
			MetadataSources ms = new MetadataSources( ssr );
			ms.addResource( "org/hibernate/orm/test/annotations/enumerated/ormXml/orm.xml" );

			Metadata metadata = ms.buildMetadata();

			Type bindingPropertyType = metadata.getEntityBinding( BookWithOrmEnum.class.getName() )
					.getProperty( "bindingStringEnum" )
					.getType();

			final JdbcTypeRegistry jdbcTypeRegistry = metadata.getDatabase()
					.getTypeConfiguration()
					.getJdbcTypeRegistry();
			BasicTypeImpl<?> enumMapping = ExtraAssertions.assertTyping( BasicTypeImpl.class, bindingPropertyType );
			assertEquals(
					jdbcTypeRegistry.getDescriptor( jdbcTypeRegistry.hasRegisteredDescriptor( ENUM ) ? ENUM : VARCHAR ),
					jdbcTypeRegistry.getDescriptor( enumMapping.getJdbcType().getDefaultSqlTypeCode() )
			);
		}
		finally {
			ServiceRegistryBuilder.destroy( ssr );
		}
	}
}
