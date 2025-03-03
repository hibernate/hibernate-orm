/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.generics;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Table;

public class Classes {

	@Embeddable
	@Table(name="Edition")
	public static class Edition<T> {
		T name;
	}


	@Entity(name = "Book")
	@Table(name="Book")
	public static class Book {
		@Id
		@GeneratedValue(strategy=GenerationType.AUTO)
		Long id;

		@Embedded
		Edition<String> edition;
	}

	@Entity(name = "PopularBook")
	@Table(name="PopularBook")
	public static class PopularBook {
		@Id
		@GeneratedValue(strategy=GenerationType.AUTO)
		Long id;

		@ElementCollection
		@JoinTable(name="PopularBook_Editions",joinColumns={@JoinColumn(name="PopularBook_id")})

		Set<Edition<String>> editions = new HashSet<Edition<String>>();
	}
}
