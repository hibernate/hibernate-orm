package org.hibernate.test.generatedkeys.identity;


/**
 * @author Steve Ebersole
 */
public class MySibling {
	private Long id;
	private String name;
	private MyEntity entity;

	public MySibling() {
	}

	public MySibling(String name) {
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

	public MyEntity getEntity() {
		return entity;
	}

	public void setEntity(MyEntity entity) {
		this.entity = entity;
	}
}
