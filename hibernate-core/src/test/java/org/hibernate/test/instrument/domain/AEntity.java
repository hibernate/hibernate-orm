package org.hibernate.test.instrument.domain;

import javax.persistence.*;

@javax.persistence.Entity(name = "A")
@Table(name = "A")
public class AEntity {
	@Id
	private long oid;

	@Column(name = "A")
	private String a;

	public String getA() {
		return a;
	}

	public void setA(String a) {
		this.a = a;
	}

	public long getOid() {
		return oid;
	}

	public void setOid(long oid) {
		this.oid = oid;
	}
}
