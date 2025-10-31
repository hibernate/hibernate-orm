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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


/**
 * Originally written to verify fix for HHH-10172
 *
 * @author Steve Ebersole
 */
public class GetAndIsVariantGetterTest {
	private static StandardServiceRegistry ssr;

	@BeforeAll
	public static void prepare() {
		ssr = ServiceRegistryUtil.serviceRegistry();
	}

	@AfterAll
	public static void release() {
		if ( ssr != null ) {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	@JiraKey(value = "HHH-10172")
	public void testHbmXml() {
		MappingException mappingException = assertThrows( MappingException.class, () ->
				new MetadataSources( ssr )
						.addResource( "org/hibernate/property/TheEntity.hbm.xml" )
						.buildMetadata()
		);
		assertThat( mappingException.getMessage() ).endsWith( "variants of getter for property 'id'" );
	}

	@Test
	@JiraKey(value = "HHH-10172")
	public void testAnnotations() {
		MappingException mappingException = assertThrows( MappingException.class, () ->
				new MetadataSources( ssr )
						.addAnnotatedClass( TheEntity.class )
						.buildMetadata()
		);
		assertThat( mappingException.getMessage() ).startsWith( "Ambiguous persistent property methods" );
	}

	@Test
	@JiraKey(value = "HHH-10242")
	public void testAnnotationsCorrected() {
		Metadata metadata = new MetadataSources( ssr )
				.addAnnotatedClass( TheEntity2.class )
				.buildMetadata();
		assertThat( metadata.getEntityBinding( TheEntity2.class.getName() ).getIdentifier() ).isNotNull();
		assertThat( metadata.getEntityBinding( TheEntity2.class.getName() ).getIdentifierProperty() ).isNotNull();
	}

	@Test
	@JiraKey(value = "HHH-10309")
	public void testAnnotationsFieldAccess() {
		// this one should be ok because the AccessType is FIELD
		Metadata metadata = new MetadataSources( ssr )
				.addAnnotatedClass( AnotherEntity.class )
				.buildMetadata();
		assertThat( metadata.getEntityBinding( AnotherEntity.class.getName() ).getIdentifier() ).isNotNull();
		assertThat( metadata.getEntityBinding( AnotherEntity.class.getName() ).getIdentifierProperty() ).isNotNull();
	}

	@Test
	@JiraKey(value = "HHH-12046")
	public void testInstanceStaticConflict() {
		Metadata metadata = new MetadataSources( ssr )
				.addAnnotatedClass( InstanceStaticEntity.class )
				.buildMetadata();
		assertThat( metadata.getEntityBinding( InstanceStaticEntity.class.getName() ).getIdentifier() ).isNotNull();
		assertThat( metadata.getEntityBinding( InstanceStaticEntity.class.getName() ).getIdentifierProperty() ).isNotNull();
		assertThat( metadata.getEntityBinding( InstanceStaticEntity.class.getName() ).hasProperty( "foo" ) ).isTrue();
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
