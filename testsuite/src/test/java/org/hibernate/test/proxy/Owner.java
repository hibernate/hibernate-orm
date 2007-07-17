package org.hibernate.test.proxy;

import java.io.Serializable;

/**
 * @author Steve Ebersole
 */
public class Owner implements Serializable {
	private Long id;
	private String name;

	public Owner() {
	}

	public Owner(String name) {
		this.name = name;
	}

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
