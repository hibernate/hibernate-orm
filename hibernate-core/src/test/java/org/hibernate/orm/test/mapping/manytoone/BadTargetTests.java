/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.manytoone;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.MappingException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;


import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@Jira( "https://hibernate.atlassian.net/browse/HHH-7202" )
@ServiceRegistry
public class BadTargetTests {
	@Test
	void testToOne(ServiceRegistryScope registryScope) {
		var model = (MetadataImplementor) new MetadataSources( registryScope.getRegistry() )
				.addAnnotatedClasses( Foo.class, Bar.class, Baz.class )
				.buildMetadata();
		try (var sf = model.buildSessionFactory()) {
			fail( "Expecting a failure" );
		}
		catch (MappingException expected) {
		}
	}

	@Test
	void testToMany(ServiceRegistryScope registryScope) {
		var model = (MetadataImplementor) new MetadataSources( registryScope.getRegistry() )
				.addAnnotatedClasses( Brazos.class, Bar.class, Baz.class )
				.buildMetadata();
		try (var sf = model.buildSessionFactory()) {
			fail( "Expecting a failure" );
		}
		catch (MappingException expected) {
			assertThat( expected.getMessage() ).startsWith( "Plural attribute [" );
			assertThat( expected.getMessage() ).contains( "Brazos.bars] was mapped with targetEntity=`" );
		}
	}

	@Test
	void testToManyGetter(ServiceRegistryScope registryScope) {
		var model = (MetadataImplementor) new MetadataSources( registryScope.getRegistry() )
				.addAnnotatedClasses( Brazos2.class, Bar.class, Baz.class )
				.buildMetadata();
		try (var sf = model.buildSessionFactory()) {
			fail( "Expecting a failure" );
		}
		catch (MappingException expected) {
			assertThat( expected.getMessage() ).startsWith( "Plural attribute [" );
			assertThat( expected.getMessage() ).contains( "Brazos2.bars] was mapped with targetEntity=`" );
		}
	}

	@Entity
	public static class Foo {
		@Id
		private Integer id;
		private String name;
		@ManyToOne(targetEntity = Baz.class)
		@JoinColumn(name = "bar_fk")
		private Bar bar;

		public Foo() {
		}

		public Foo(Integer id, String name, Bar bar) {
			this.id = id;
			this.name = name;
			this.bar = bar;
		}
	}

	@Entity
	public static class Bar {
		@Id
		private Integer id;
		private String name;

		public Bar() {
		}

		public Bar(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity
	public static class Baz {
		@Id
		private Integer id;
		private String name;
	}

	@Entity
	public static class Brazos {
		@Id
		private Integer id;
		private String name;
		@OneToMany(targetEntity = Baz.class)
		@JoinColumn(name = "bar_fk")
		private Set<Bar> bars;

		public Brazos() {
		}
	}

	@Entity
	public static class Brazos2 {
		private Integer id;
		private String name;
		private Set<Bar> bars;

		public Brazos2() {
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@OneToMany(targetEntity = Baz.class)
		@JoinColumn(name = "bar_fk")
		public Set<Bar> getBars() {
			return bars;
		}

		public void setBars(Set<Bar> bars) {
			this.bars = bars;
		}
	}

}
