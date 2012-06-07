package org.hibernate.test.idprops;


/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class Person {
	private Long pk;
	private String name;
	private int id;

	public Person() {
	}

	public Person(Long pk, String name, int id) {
		this.pk = pk;
		this.name = name;
		this.id = id;
	}

	public Long getPk() {
		return pk;
	}

	public void setPk(Long pk) {
		this.pk = pk;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
}
