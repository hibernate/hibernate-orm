/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.foreignkeys;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.mapping.Table;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Yanming Zhou
 */
@JiraKey(value = "HHH-14230")
public class HHH14230 {

	private static final String TABLE_NAME = "test_entity";
	private static final String JOIN_COLUMN_NAME = "parent";

	@Test
	public void test() {
		try (StandardServiceRegistry serviceRegistry = ServiceRegistryUtil.serviceRegistry()) {
			Metadata metadata = new MetadataSources( serviceRegistry )
					.addAnnotatedClass( TestEntity.class ).buildMetadata();
			Table table = StreamSupport.stream( metadata.getDatabase().getNamespaces().spliterator(), false )
					.flatMap( namespace -> namespace.getTables().stream() )
					.filter( t -> t.getName().equals( TABLE_NAME ) ).findFirst().orElse( null );
			assertThat( table ).isNotNull();
			assertThat( table.getForeignKeyCollection() ).hasSize( 1 );

			// ClassCastException before HHH-14230
			assertThat( table.getForeignKeys().keySet().iterator().next().toString() ).contains( JOIN_COLUMN_NAME );
		}
	}

	@Entity
	@jakarta.persistence.Table(name = TABLE_NAME)
	public static class TestEntity {

		@Id
		private Long id;

		@ManyToOne
		@JoinColumn(name = JOIN_COLUMN_NAME)
		private TestEntity parent;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public TestEntity getParent() {
			return parent;
		}

		public void setParent(TestEntity parent) {
			this.parent = parent;
		}

	}

}
