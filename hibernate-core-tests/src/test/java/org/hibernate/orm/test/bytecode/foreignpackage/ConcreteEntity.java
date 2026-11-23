package org.hibernate.orm.test.bytecode.foreignpackage;

import org.hibernate.orm.test.bytecode.SuperclassEntity;

import jakarta.persistence.Entity;

@Entity
public class ConcreteEntity extends SuperclassEntity {
	protected String bname;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getBname() {
		return bname;
	}

	public void setBname(String bname) {
		this.bname = bname;
	}
}
