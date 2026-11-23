package org.hibernate.orm.test.version.mappedsuperclass;

/**
 * @author Andrea Boriero
 */
public class TestEntity extends AbstractEntity {
	String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
