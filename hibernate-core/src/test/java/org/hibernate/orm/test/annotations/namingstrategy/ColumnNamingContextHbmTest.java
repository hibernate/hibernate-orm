/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.namingstrategy;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@BaseUnitTest
public class ColumnNamingContextHbmTest {
	private ServiceRegistry serviceRegistry;

	@BeforeEach
	public void setUp() {
		serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry();
	}

	@AfterEach
	public void tearDown() {
		if ( serviceRegistry != null ) {
			ServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Test
	public void testColumnNamingContextForHbmMappings() {
		final Metadata metadata = new MetadataSources( serviceRegistry )
				.addResource( "org/hibernate/orm/test/annotations/namingstrategy/ColumnNamingContextHbmTest.hbm.xml" )
				.getMetadataBuilder()
				.applyPhysicalNamingStrategy( new ContextNamingStrategy() )
				.build();

		final PersistentClass employeeBinding = metadata.getEntityBinding( HbmEmployee.class.getName() );
		assertThat( employeeBinding.getKey().getColumns().get( 0 ).getName() )
				.isEqualTo( "HbmEmployee_person_id" );
		assertThat( employeeBinding.getProperty( "manager" ).getSelectables().get( 0 ).getText() )
				.isEqualTo( "HbmEmployee_manager_fk" );
	}

	public static class HbmPerson {
		private Long id;
		private String name;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	public static class HbmEmployee extends HbmPerson {
		private HbmEmployee manager;

		public HbmEmployee getManager() {
			return manager;
		}

		public void setManager(HbmEmployee manager) {
			this.manager = manager;
		}
	}
}
