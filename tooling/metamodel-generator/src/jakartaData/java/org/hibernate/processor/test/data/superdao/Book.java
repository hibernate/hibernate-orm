/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.data.superdao;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.NaturalId;
import org.hibernate.processor.test.hqlsql.Publisher;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
public class Book {
	@Id String isbn;
	@NaturalId String title;
	String text;
	@NaturalId String authorName;
	@ManyToOne
	Publisher publisher;
	BigDecimal price;
	int pages;
	LocalDate publicationDate;
}
