package org.hibernate.jpa.test.multimappingperclass;

import java.io.Serializable;

public class Entity implements Serializable {

	private static final long serialVersionUID = -3680457819805961805L;

	private Long id;

	private String name;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
