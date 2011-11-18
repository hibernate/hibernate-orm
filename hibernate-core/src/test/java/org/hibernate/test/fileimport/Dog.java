package org.hibernate.test.fileimport;


/**
 * @author Emmanuel Bernard
 */
public class Dog {
	private Integer id;
	private Human master;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Human getMaster() {
		return master;
	}

	public void setMaster(Human master) {
		this.master = master;
	}
}
