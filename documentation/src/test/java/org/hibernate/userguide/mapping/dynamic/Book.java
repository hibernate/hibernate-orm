package org.hibernate.userguide.mapping.dynamic;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

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
