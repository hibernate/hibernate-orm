package org.hibernate.test.bytecode.enhancement.otherentityentrycontext;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Created by barreiro on 12/9/15.
 */
@Entity
public class Parent {
	private Integer id;
	private String name;

	public Parent() {
	}

	public Parent(Integer id, String name) {
		this.id = id;
		this.name = name;
	}
	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
