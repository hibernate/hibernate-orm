/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph.named.parsed.simple;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedAttributeNode;
import org.hibernate.DuplicateMappingException;
import org.hibernate.annotations.NamedEntityGraph;
import org.hibernate.boot.MetadataSources;
import org.hibernate.testing.orm.junit.NotImplementedYet;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;


/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class DuplicateNameTests {
	@Test
	@ServiceRegistry
	void testDuplicateNamesBaseline(ServiceRegistryScope registryScope) {
		final MetadataSources metadataSources = new MetadataSources( registryScope.getRegistry() )
				.addAnnotatedClasses( BaselineEntity.class );
		try {
			metadataSources.buildMetadata();
			fail( "Expecting a failure" );
		}
		catch (DuplicateMappingException expected) {
		}
	}

	@Test
	@ServiceRegistry
	@NotImplementedYet( reason = "Support for parsed NamedEntityGraph is not implemented" )
	void testDuplicateNames(ServiceRegistryScope registryScope) {
		final MetadataSources metadataSources = new MetadataSources( registryScope.getRegistry() )
				.addAnnotatedClasses( TestEntity.class );
		try {
			metadataSources.buildMetadata();
			fail( "Expecting a failure" );
		}
		catch (DuplicateMappingException expected) {
		}
	}

	@NamedEntityGraph( name = "test-id-name", graph = "(id, name)" )
	@jakarta.persistence.NamedEntityGraph(
			name = "test-id-name",
			attributeNodes = {
					@NamedAttributeNode( "id" ),
					@NamedAttributeNode( "name" )
			}
	)
	@Entity
	public static class TestEntity {
		@Id
		private Integer id;
		private String name;
	}

	@jakarta.persistence.NamedEntityGraph(
			name = "test-id-name",
			attributeNodes = {
					@NamedAttributeNode( "id" ),
					@NamedAttributeNode( "name" )
			}
	)
	@jakarta.persistence.NamedEntityGraph(
			name = "test-id-name",
			attributeNodes = {
					@NamedAttributeNode( "id" ),
					@NamedAttributeNode( "name" )
			}
	)
	@Entity
	public static class BaselineEntity {
		@Id
		private Integer id;
		private String name;
	}
}
