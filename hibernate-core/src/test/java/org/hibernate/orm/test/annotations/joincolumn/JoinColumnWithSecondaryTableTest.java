/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.joincolumn;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

@DomainModel(
		annotatedClasses = {
				JoinColumnWithSecondaryTableTest.Being.class,
				JoinColumnWithSecondaryTableTest.Animal.class,
				JoinColumnWithSecondaryTableTest.Cat.class,
				JoinColumnWithSecondaryTableTest.Toy.class
		}
)
@SessionFactory
@JiraKey( value = "HHH-15111")
public class JoinColumnWithSecondaryTableTest {

	@Test
	public void testIt(SessionFactoryScope scope){

	}

	@Entity
	@Table(name = "being")
	@Inheritance
	@DiscriminatorColumn(name = "type")
	public static abstract class Being {

		@Id
		private Long id;
	}

	@Entity
	@SecondaryTable(name = "animal")
	public static abstract class Animal extends Being {
		@Column(name = "uuid", table = "animal")
		private String uuid;
	}

	@Entity
	@SecondaryTable(name = "cat")
	@DiscriminatorValue(value = "CAT")
	public static class Cat extends Animal {
		@Column(name = "name", table = "cat")
		private String name;
	}

	@Entity
	public static class Toy {
		@Id
		private Long id;

		@ManyToOne
		@JoinColumn(name = "animal_uuid", referencedColumnName = "uuid")
		private Cat cat;
	}
}
