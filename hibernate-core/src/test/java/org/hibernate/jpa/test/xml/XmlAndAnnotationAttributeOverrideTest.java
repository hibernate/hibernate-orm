/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.xml;

import static org.assertj.core.api.Assertions.assertThat;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.hibernate.test.util.SchemaUtil;
import org.junit.Test;

public class XmlAndAnnotationAttributeOverrideTest extends BaseEntityManagerFunctionalTestCase {
	@Test
	@TestForIssue(jiraKey = "HHH-14827")
	public void testDerivedClassAttributeOverriding() {
		assertThat( SchemaUtil.getColumnNames( entityManagerFactory(), DerivedEntityType.class ) )
				.contains( "custom_name" )
				.doesNotContain( "name" );
	}

	@Test
	public void testEmbeddedAttributeOverriding() {
		assertThat( SchemaUtil.getColumnNames( entityManagerFactory(), DerivedEntityType.class ) )
				.contains( "custom_embeddable_name" )
				.doesNotContain( "embeddable_name" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { MappedSuperclassType.class, DerivedEntityType.class, EmbeddableType.class };
	}

	@Override
	public String[] getEjb3DD() {
		return new String[] {
				// Using an empty orm.xml: the mere presence of an orm.xml used to trigger the bug,
				// regardless of its content.
				"org/hibernate/jpa/test/xml/orm-empty.xml"
		};
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
