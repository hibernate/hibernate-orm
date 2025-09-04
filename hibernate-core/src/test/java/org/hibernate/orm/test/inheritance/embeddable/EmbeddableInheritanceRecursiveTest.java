/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance.embeddable;

import org.hibernate.MappingException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@Jira("https://hibernate.atlassian.net/browse/HHH-19648")
public class EmbeddableInheritanceRecursiveTest {
	@Test
	public void testSimpleRecursiveEmbedded() {
		final StandardServiceRegistryBuilder registryBuilder = ServiceRegistryUtil.serviceRegistryBuilder();
		final MetadataSources metadataSources = new MetadataSources( registryBuilder.build() )
				.addAnnotatedClass( Root1.class )
				.addAnnotatedClass( Entity1.class );
		try {
			final Metadata metadata = metadataSources.buildMetadata();
			fail( "Expected MappingException due to recursive embeddable mapping" );
		}
		catch (Exception e) {
			assertThat( e ).isInstanceOf( MappingException.class );
			assertThat( e.getMessage() )
					.contains( "Recursive embeddable mapping detected" )
					.contains( Root1.class.getName() );
		}
	}

	@Test
	public void testChildWithRootProp() {
		final StandardServiceRegistryBuilder registryBuilder = ServiceRegistryUtil.serviceRegistryBuilder();
		final MetadataSources metadataSources = new MetadataSources( registryBuilder.build() )
				.addAnnotatedClass( Root2.class )
				.addAnnotatedClass( Child2.class )
				.addAnnotatedClass( Entity2.class );
		try {
			final Metadata metadata = metadataSources.buildMetadata();
			fail( "Expected MappingException due to recursive embeddable mapping" );
		}
		catch (Exception e) {
			assertThat( e ).isInstanceOf( MappingException.class );
			assertThat( e.getMessage() )
					.contains( "Recursive embeddable mapping detected" )
					.contains( Root2.class.getName() );
		}
	}

	@Test
	public void testRootWithChildProp() {
		final StandardServiceRegistryBuilder registryBuilder = ServiceRegistryUtil.serviceRegistryBuilder();
		final MetadataSources metadataSources = new MetadataSources( registryBuilder.build() )
				.addAnnotatedClass( Root3.class )
				.addAnnotatedClass( Child3.class )
				.addAnnotatedClass( Entity3.class );
		try {
			final Metadata metadata = metadataSources.buildMetadata();
			fail( "Expected MappingException due to recursive embeddable mapping" );
		}
		catch (Exception e) {
			assertThat( e ).isInstanceOf( MappingException.class );
			assertThat( e.getMessage() )
					.contains( "Recursive embeddable mapping detected" )
					.contains( Root3.class.getName() );
		}
	}

	@Test
	public void testMidEmbedded() {
		final StandardServiceRegistryBuilder registryBuilder = ServiceRegistryUtil.serviceRegistryBuilder();
		final MetadataSources metadataSources = new MetadataSources( registryBuilder.build() )
				.addAnnotatedClass( Root4.class )
				.addAnnotatedClass( Mid4.class )
				.addAnnotatedClass( Child4.class )
				.addAnnotatedClass( Entity4.class );
		try {
			final Metadata metadata = metadataSources.buildMetadata();
			fail( "Expected MappingException due to recursive embeddable mapping" );
		}
		catch (Exception e) {
			assertThat( e ).isInstanceOf( MappingException.class );
			assertThat( e.getMessage() )
					.contains( "Recursive embeddable mapping detected" )
					.contains( Root4.class.getName() );
		}
	}

	@Test
	public void testUnrelatedRecursive() {
		final StandardServiceRegistryBuilder registryBuilder = ServiceRegistryUtil.serviceRegistryBuilder();
		final MetadataSources metadataSources = new MetadataSources( registryBuilder.build() )
				.addAnnotatedClass( EmbA.class )
				.addAnnotatedClass( EmbB.class )
				.addAnnotatedClass( Entity5.class );
		try {
			final Metadata metadata = metadataSources.buildMetadata();
			fail( "Expected MappingException due to recursive embeddable mapping" );
		}
		catch (Exception e) {
			assertThat( e ).isInstanceOf( MappingException.class );
			assertThat( e.getMessage() )
					.contains( "Recursive embeddable mapping detected" )
					.contains( EmbA.class.getName() );
		}
	}

	@Embeddable
	static class Root1 {
		String root1Prop;

		@Embedded
		Root1 nested1;
	}

	@Entity(name = "Entity1")
	static class Entity1 {
		@Id
		private Long id;

		@Embedded
		private Root1 root1;
	}

	@Embeddable
	static class Root2 {
		String root2Prop;
	}

	@Embeddable
	static class Child2 extends Root2 {
		String child2Prop;

		@Embedded
		Root2 nested2;
	}

	@Entity(name = "Entity2")
	static class Entity2 {
		@Id
		private Long id;

		@Embedded
		private Root2 root2;
	}

	@Embeddable
	static class Root3 {
		String root3Prop;

		@Embedded
		Child3 nested3;
	}

	@Embeddable
	static class Child3 extends Root3 {
		String child3Prop;
	}

	@Entity(name = "Entity3")
	static class Entity3 {
		@Id
		private Long id;

		@Embedded
		private Root3 root3;
	}

	@Embeddable
	static class Root4 {
		String root4Prop;
	}

	@Embeddable
	static class Mid4 extends Root4 {
	}

	@Embeddable
	static class Child4 extends Mid4 {
		String child4Prop;

		@Embedded
		Mid4 nested4;
	}

	@Entity(name = "Entity4")
	static class Entity4 {
		@Id
		private Long id;

		@Embedded
		private Root4 root4;
	}

	@Embeddable
	static class EmbA {
		String emb1;

		@Embedded
		EmbB embB;
	}

	@Embeddable
	static class EmbB {
		String embB;

		@Embedded
		EmbA embA;
	}

	@Entity(name = "Entity5")
	static class Entity5 {
		@Id
		private Long id;

		@Embedded
		private EmbA embA;
	}
}
