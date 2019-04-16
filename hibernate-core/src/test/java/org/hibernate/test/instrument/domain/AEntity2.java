package org.hibernate.test.instrument.domain;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;

@javax.persistence.Entity(name = "A2")
@Table(name = "A2")
public class AEntity2 {
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
