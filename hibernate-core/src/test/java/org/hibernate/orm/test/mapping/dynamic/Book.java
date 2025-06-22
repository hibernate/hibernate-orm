/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.dynamic;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

//tag::access-field-mapping-example[]
@Entity(name = "Book")
public class Book {

	@Id
	@GeneratedValue
	private Long id;

	private String title;

	private String author;

	//Getters and setters are omitted for brevity
	//end::access-field-mapping-example[]

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}
	//tag::access-field-mapping-example[]
}
