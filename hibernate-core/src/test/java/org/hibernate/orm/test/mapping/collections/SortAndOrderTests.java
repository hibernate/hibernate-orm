/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections;

import java.util.Set;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.SortNatural;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@JiraKey( "https://hibernate.atlassian.net/browse/HHH-9688" )
@ServiceRegistry
public class SortAndOrderTests {

	@Test
	void test(ServiceRegistryScope scope) {
		final StandardServiceRegistry registry = scope.getRegistry();
		final MetadataSources sources = new MetadataSources( registry ).addAnnotatedClass( AnEntity.class );

		try {
			sources.buildMetadata();
			fail( "Expecting to fail" );
		}
		catch (AnnotationException expected) {
			assertThat( expected ).hasMessageContaining( "both sorted and ordered" );
		}
	}

	@Entity( name = "AnEntity" )
	@Table( name = "t_entity" )
	public static class AnEntity {
		@Id
		private Integer id;
		@Basic
		private String name;

		@ElementCollection
		@SortNatural
		@OrderBy
		private Set<String> aliases;

		private AnEntity() {
			// for use by Hibernate
		}

		public AnEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
