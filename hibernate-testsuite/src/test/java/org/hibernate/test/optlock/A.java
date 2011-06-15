package org.hibernate.test.optlock;


import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

@Table(name = "taba")
@Entity
public class A {
	private static final long serialVersionUID = 1L;

	private int id;
	private int version;
	private String descr;

	public A() {
	}

	public A(int id, String descr) {
		this.id = id;
		this.descr = descr;
	}

	@Id
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Version
	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	@Basic
	public String getDescr() {
		return descr;
	}

	public void setDescr(String descr) {
		this.descr = descr;
	}
}

