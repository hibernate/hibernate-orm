package org.hibernate.test.jpa;


/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class MyEntity {
	private Long id;
	private String name;
	private MyEntity other;

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

	public MyEntity getOther() {
		return other;
	}

	public void setOther(MyEntity other) {
		this.other = other;
	}
}
