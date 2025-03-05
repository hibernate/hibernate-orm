/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.refcolnames.misc;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@DomainModel(annotatedClasses = {Misc5Test.Animal.class})
@SessionFactory
@JiraKey(value = "HHH-14014")
public class Misc5Test {
	@Test
	void test(SessionFactoryScope scope) {
		scope.inTransaction(x -> {
			Animal a = new Animal();
			a.name = "Dragon";
			a.key = "xxxx";
			Animal b = new Animal();
			b.name = "Lizard";
			b.key = "yyyy";
			a.relatives.add(b);
		});
	}

	@Entity
	@Table(name = "animal")
	public static class Animal implements Serializable {
		@Id
		@Column(name = "id")
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Integer id;
		@Column(name = "name", nullable = false, unique = true)
		private String name;

		@Column(name = "ukey", unique = true)
		private String key;

		@ManyToMany
		@JoinTable(name = "relatives", joinColumns = @JoinColumn(name = "ukey1", referencedColumnName = "ukey"), inverseJoinColumns = @JoinColumn(name = "ukey2", referencedColumnName = "ukey"))
		private Set<Animal> relatives = new HashSet<>();
	}
}
