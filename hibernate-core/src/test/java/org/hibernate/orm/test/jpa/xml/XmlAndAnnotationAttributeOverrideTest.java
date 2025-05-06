/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.xml;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.hibernate.orm.test.util.SchemaUtil;

import org.junit.jupiter.api.Test;

@Jpa(annotatedClasses = {
		XmlAndAnnotationAttributeOverrideTest.MappedSuperclassType.class, XmlAndAnnotationAttributeOverrideTest.DerivedEntityType.class, XmlAndAnnotationAttributeOverrideTest.EmbeddableType.class
},
		// Using an empty orm.xml: the mere presence of an orm.xml used to trigger the bug,
		// regardless of its content
		xmlMappings = "org/hibernate/jpa/test/xml/orm-empty.xml"

)
public class XmlAndAnnotationAttributeOverrideTest {
	@Test
	@JiraKey(value = "HHH-14827")
	public void testDerivedClassAttributeOverriding(EntityManagerFactoryScope scope) {
		assertThat( SchemaUtil.getColumnNames( scope.getEntityManagerFactory(), DerivedEntityType.class ) )
				.contains( "custom_name" )
				.doesNotContain( "name" );
	}

	@Test
	public void testEmbeddedAttributeOverriding(EntityManagerFactoryScope scope) {
		assertThat( SchemaUtil.getColumnNames( scope.getEntityManagerFactory(), DerivedEntityType.class ) )
				.contains( "custom_embeddable_name" )
				.doesNotContain( "embeddable_name" );
	}

	@MappedSuperclass
	public static class MappedSuperclassType {

		private String name;

		public MappedSuperclassType() {
		}

		public MappedSuperclassType(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "derivedentity")
	@AttributeOverride(name = "name", column = @Column(name = "custom_name"))
	public static class DerivedEntityType extends MappedSuperclassType {

		@Id
		@GeneratedValue
		private long id;

		@Embedded
		@AttributeOverride(name = "embeddableName", column = @Column(name = "custom_embeddable_name"))
		private EmbeddableType embedded;

		public DerivedEntityType() {
		}

		public DerivedEntityType(String name) {
			super( name );
		}

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public EmbeddableType getEmbedded() {
			return embedded;
		}

		public void setEmbedded(EmbeddableType embedded) {
			this.embedded = embedded;
		}
	}

	@Embeddable
	public static class EmbeddableType {
		private String embeddableName;

		public String getEmbeddableName() {
			return embeddableName;
		}

		public void setEmbeddableName(String embeddableName) {
			this.embeddableName = embeddableName;
		}
	}
}
