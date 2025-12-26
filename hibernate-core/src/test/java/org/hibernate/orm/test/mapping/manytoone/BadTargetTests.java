/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.manytoone;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.MappingException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.fail;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_AUTO;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@Jira( "https://hibernate.atlassian.net/browse/HHH-7202" )
@ServiceRegistry(settings = @Setting( name = HBM2DDL_AUTO, value = "create-drop" ))
public class BadTargetTests {
	@Test
	void testIt(ServiceRegistryScope registryScope) {
		var model = (MetadataImplementor) new MetadataSources( registryScope.getRegistry() )
				.addAnnotatedClasses( Foo.class, Bar.class, Baz.class )
				.buildMetadata();
		try (var sf = model.buildSessionFactory()) {
			fail( "Expecting a failure" );
		}
		catch (MappingException expected) {
		}
	}

	@Entity(name="Foo")
	@Table(name="foos")
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

	@Entity(name="Bar")
	@Table(name="bars")
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

	@Entity(name="Baz")
	@Table(name="Baz")
	public static class Baz {
		@Id
		private Integer id;
		private String name;
	}
}
