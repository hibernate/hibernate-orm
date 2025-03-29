/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.property;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;

import org.hibernate.MappingException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.internal.util.ReflectHelper;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Originally written to verify fix for HHH-10172
 *
 * @author Steve Ebersole
 */
public class GetAndIsVariantGetterTest {
	private static StandardServiceRegistry ssr;

	@BeforeClass
	public static void prepare() {
		ssr = ServiceRegistryUtil.serviceRegistry();
	}

	@AfterClass
	public static void release() {
		if ( ssr != null ) {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	@JiraKey( value = "HHH-10172" )
	public void testHbmXml() {
		try {
			new MetadataSources( ssr )
					.addResource( "org/hibernate/property/TheEntity.hbm.xml" )
					.buildMetadata();
			fail( "Expecting a failure" );
		}
		catch (MappingException e) {
			assertThat( e.getMessage(), endsWith( "variants of getter for property 'id'" ) );
		}
	}

	@Test
	@JiraKey( value = "HHH-10172" )
	public void testAnnotations() {
		try {
			new MetadataSources( ssr )
					.addAnnotatedClass( TheEntity.class )
					.buildMetadata();
			fail( "Expecting a failure" );
		}
		catch (MappingException e) {
			assertThat( e.getMessage(), startsWith( "Ambiguous persistent property methods" ) );
		}
	}

	@Test
	@JiraKey( value = "HHH-10242" )
	public void testAnnotationsCorrected() {
		Metadata metadata = new MetadataSources( ssr )
				.addAnnotatedClass( TheEntity2.class )
				.buildMetadata();
		assertNotNull( metadata.getEntityBinding( TheEntity2.class.getName() ).getIdentifier() );
		assertNotNull( metadata.getEntityBinding( TheEntity2.class.getName() ).getIdentifierProperty() );
	}

	@Test
	@JiraKey( value = "HHH-10309" )
	public void testAnnotationsFieldAccess() {
		// this one should be ok because the AccessType is FIELD
		Metadata metadata = new MetadataSources( ssr )
				.addAnnotatedClass( AnotherEntity.class )
				.buildMetadata();
		assertNotNull( metadata.getEntityBinding( AnotherEntity.class.getName() ).getIdentifier() );
		assertNotNull( metadata.getEntityBinding( AnotherEntity.class.getName() ).getIdentifierProperty() );
	}

	@Test
	@JiraKey( value = "HHH-12046" )
	public void testInstanceStaticConflict() {
		Metadata metadata = new MetadataSources( ssr )
				.addAnnotatedClass( InstanceStaticEntity.class )
				.buildMetadata();
		assertNotNull( metadata.getEntityBinding( InstanceStaticEntity.class.getName() ).getIdentifier() );
		assertNotNull( metadata.getEntityBinding( InstanceStaticEntity.class.getName() ).getIdentifierProperty() );
		assertTrue( metadata.getEntityBinding( InstanceStaticEntity.class.getName() ).hasProperty("foo") );
		ReflectHelper.findGetterMethod( InstanceStaticEntity.class, "foo" );
	}

	@Entity
	public static class TheEntity {
		private Integer id;

		public boolean isId() {
			return false;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	@Entity
	public static class TheEntity2 {
		private Integer id;

		@Transient
		public boolean isId() {
			return false;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	@Entity
	@Access(AccessType.PROPERTY)
	public static class AnotherEntity {
		@Id
		@Access(AccessType.FIELD)
		private Integer id;

		public boolean isId() {
			return false;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	@Entity
	public static class InstanceStaticEntity {

		private Integer id;
		private boolean foo;

		@Id
		public Integer getId() {
			return id;
		}
		public void setId(Integer id) {
			this.id = id;
		}

		public boolean isFoo() {
			return this.foo;
		}
		public void setFoo(boolean foo) {
			this.foo = foo;
		}

		public static Object getFoo() {
			return null;
		}

		public static boolean isId() {
			return false;
		}
	}
}
