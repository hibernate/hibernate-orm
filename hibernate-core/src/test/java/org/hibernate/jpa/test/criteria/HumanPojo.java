package org.hibernate.jpa.test.criteria;

public class HumanPojo {
	public HumanPojo(Long id, String name, String dateBorn) {
		this.id = id;
		this.name = name;
		this.dateBorn = dateBorn;
	}

	private Long id;
	private String name;
	private String dateBorn;

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

	public String getDateBorn() {
		return dateBorn;
	}

	public void setDateBorn(String dateBorn) {
		this.dateBorn = dateBorn;
	}
}