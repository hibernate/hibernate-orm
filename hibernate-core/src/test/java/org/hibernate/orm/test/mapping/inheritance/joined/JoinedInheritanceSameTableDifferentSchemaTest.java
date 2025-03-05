/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.inheritance.joined;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Jira("https://hibernate.atlassian.net/browse/HHH-7134")
public class JoinedInheritanceSameTableDifferentSchemaTest {
	@Test
	public void testMapping() {
		StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry();
		try {
			final Metadata metadata = new MetadataSources( ServiceRegistryUtil.serviceRegistry() )
					.addAnnotatedClass( EntityA.class )
					.addAnnotatedClass( EntityB.class )
					.buildMetadata();
			org.hibernate.mapping.Table entity1Table = metadata.getEntityBinding( EntityA.class.getName() ).getTable();
			org.hibernate.mapping.Table entity2Table = metadata.getEntityBinding( EntityB.class.getName() ).getTable();
			assertThat( entity1Table.getName() ).isEqualTo( entity2Table.getName() );
			assertThat( entity1Table.getSchema() ).isNotEqualTo( entity2Table.getSchema() );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Entity(name = "EntityA")
	@Inheritance(strategy = InheritanceType.JOINED)
	@Table(schema = "schema_1", name = "my_table")
	public static class EntityA {
		@Id
		private Long id;
	}

	@Entity(name = "EntityB")
	@Table(schema = "schema_2", name = "my_table")
	public static class EntityB extends EntityA {
		private String name;
	}
}
