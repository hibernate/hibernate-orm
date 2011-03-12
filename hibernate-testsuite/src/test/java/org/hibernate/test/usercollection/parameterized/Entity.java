package org.hibernate.test.usercollection.parameterized;

import java.util.List;
import java.util.ArrayList;

/**
 * Our test entity
 *
 * @author Steve Ebersole
 */
public class Entity {
	private String name;
	private List values = new ArrayList();

	public Entity() {
	}

	public Entity(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public List getValues() {
		return values;
	}

	public void setValues(List values) {
		this.values = values;
	}
}
