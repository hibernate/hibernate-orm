/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.schemaupdate;

import java.util.Map;

import org.hibernate.mapping.PersistentClass;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.Table;

import static jakarta.persistence.DiscriminatorType.CHAR;
import static jakarta.persistence.InheritanceType.SINGLE_TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;

@JiraKey("HHH-16551")
public class CreateCharDiscriminatorTest extends BaseNonConfigCoreFunctionalTestCase {

	@org.junit.Test
	@JiraKey("HHH-16551")
	public void testCreateDiscriminatorCharColumnSize() {
		PersistentClass classMapping = metadata().getEntityBinding( Parent.class.getName() );
		final var discriminatorColumn = classMapping.getDiscriminator().getColumns().get( 0 );
		assertEquals( discriminatorColumn.getLength(), 1L );
	}

	@Override
	protected void addSettings(Map<String, Object> settings) {
		settings.put( "jakarta.persistence.validation.mode", "ddl" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Parent.class
		};
	}

	@Entity
	@Table(name = "parent")
	@Inheritance(strategy = SINGLE_TABLE)
	@DiscriminatorColumn(name = "discr", discriminatorType = CHAR, length = 2)
	@DiscriminatorValue("*")
	public static class Parent {

		@Id
		private Integer id;

		@Column
		private String name;

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
}
