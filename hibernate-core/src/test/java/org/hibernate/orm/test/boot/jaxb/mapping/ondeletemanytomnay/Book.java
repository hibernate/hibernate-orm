package org.hibernate.orm.test.boot.jaxb.mapping.ondeletemanytomnay;

public class Book {
	private Long id;
	private String title;

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
}
