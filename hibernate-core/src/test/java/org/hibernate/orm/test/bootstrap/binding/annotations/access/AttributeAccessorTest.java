/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.access;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.AttributeAccessor;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.Environment;
import org.hibernate.mapping.Property;
import org.hibernate.property.access.internal.PropertyAccessStrategyBasicImpl;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-12062")
public class AttributeAccessorTest {
	private ServiceRegistry serviceRegistry;

	@BeforeEach
	public void setUp() {
		serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( Environment.getProperties() );
	}

	@AfterEach
	public void cleanUp() {
		if ( serviceRegistry != null ) {
			ServiceRegistryBuilder.destroy( serviceRegistry );
			serviceRegistry = null;
		}
	}

	@Test
	public void testAttributeAccessorConfiguration() {
		final Metadata metadata = new MetadataSources( serviceRegistry )
				.addAnnotatedClass( Foo.class )
				.buildMetadata();

		final Property property = metadata.getEntityBinding( Foo.class.getName() ).getProperty( "name" );
		assertEquals( BasicAttributeAccessor.class.getName(), property.getPropertyAccessorName() );
	}

	@Entity(name = "Foo")
	public static class Foo {
		private Integer id;
		private String name;

		public Foo() {

		}

		public Foo(Integer id) {
			this.id = id;
		}

		public Foo(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@AttributeAccessor(strategy = BasicAttributeAccessor.class)
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	public static class BasicAttributeAccessor extends PropertyAccessStrategyBasicImpl {
		@Override
		public PropertyAccess buildPropertyAccess(Class containerJavaType, String propertyName, boolean setterRequired) {
			return super.buildPropertyAccess( containerJavaType, propertyName, setterRequired );
		}
	}
}
