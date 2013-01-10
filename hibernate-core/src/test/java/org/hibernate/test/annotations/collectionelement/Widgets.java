package org.hibernate.test.annotations.collectionelement;
import javax.persistence.Embeddable;

@Embeddable
public class Widgets {
	private String name;

	public Widgets() {

	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
