/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.Table;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static jakarta.persistence.DiscriminatorType.CHAR;
import static jakarta.persistence.InheritanceType.SINGLE_TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey("HHH-16551")
@ServiceRegistry(settings = @Setting(name="jakarta.persistence.validation.mode", value = "ddl"))
@DomainModel(annotatedClasses = CreateCharDiscriminatorTest.Parent.class)
public class CreateCharDiscriminatorTest {

	@Test
	public void testCreateDiscriminatorCharColumnSize(DomainModelScope modelScope) {
		final var classMapping = modelScope.getEntityBinding( Parent.class );
		final var discriminatorColumn = classMapping.getDiscriminator().getColumns().get( 0 );
		assertEquals( 1L, discriminatorColumn.getLength() );
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
