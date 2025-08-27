/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.identifier;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Vlad Mihalcea
 */
//tag::entity-pojo-mapping-implicit-name-example[]
@Entity
public class Book {

	@Id
	private Long id;

	private String title;

	private String author;

	//Getters and setters are omitted for brevity
//end::entity-pojo-mapping-implicit-name-example[]

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
//tag::entity-pojo-mapping-implicit-name-example[]
}
//end::entity-pojo-mapping-implicit-name-example[]
