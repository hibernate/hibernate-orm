package org.hibernate.test.instrument.domain;

import javax.persistence.Id;
import javax.persistence.Table;

@javax.persistence.Entity(name = "B2")
@Table(name = "B2")
public class BEntity2 {
	@Id
	public long oid;
	private Integer b1;
	private String b2;

	public Integer getB1() {
		return b1;
	}

	public void setB1(Integer b1) {
		this.b1 = b1;
	}

	public String getB2() {
		return b2;
	}

	public void setB2(String b2) {
		this.b2 = b2;
	}

	public long getOid() {
		return oid;
	}

	public void setOid(long oid) {
		this.oid = oid;
	}
}
