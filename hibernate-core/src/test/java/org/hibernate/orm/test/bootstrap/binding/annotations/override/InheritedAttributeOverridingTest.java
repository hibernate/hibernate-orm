/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.override;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
public class InheritedAttributeOverridingTest extends BaseUnitTestCase {
	private StandardServiceRegistry standardServiceRegistry;

	@Before
	public void buildServiceRegistry() {
		standardServiceRegistry = ServiceRegistryUtil.serviceRegistry();
	}

	@After
	public void releaseServiceRegistry() {
		if ( standardServiceRegistry != null ) {
			StandardServiceRegistryBuilder.destroy( standardServiceRegistry );
		}
	}

	@Test
	@JiraKey( value = "HHH-9485" )
	public void testInheritedAttributeOverridingMappedsuperclass() {
		Metadata metadata = new MetadataSources( standardServiceRegistry )
				.addAnnotatedClass( A.class )
				.addAnnotatedClass( B.class )
				.buildMetadata();

		( (MetadataImplementor) metadata ).orderColumns( false );
		( (MetadataImplementor) metadata ).validate();
	}

	@Test
	@JiraKey( value = "HHH-9485" )
	public void testInheritedAttributeOverridingEntity() {
		Metadata metadata = new MetadataSources( standardServiceRegistry )
				.addAnnotatedClass( C.class )
				.addAnnotatedClass( D.class )
				.buildMetadata();

		( (MetadataImplementor) metadata ).orderColumns( false );
		( (MetadataImplementor) metadata ).validate();
	}

	@MappedSuperclass
	public static class A {
		private Integer id;
		private String name;

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
	}

	@Entity( name = "B" )
	public static class B extends A {
		@Override
		public String getName() {
			return super.getName();
		}

		@Override
		public void setName(String name) {
			super.setName( name );
		}
	}


	@Entity( name = "C" )
	public static class C {
		private Integer id;
		private String name;

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
	}

	@Entity( name = "D" )
	public static class D extends C {
		@Override
		public String getName() {
			return super.getName();
		}

		@Override
		public void setName(String name) {
			super.setName( name );
		}
	}
}
