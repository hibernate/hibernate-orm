package org.hibernate.test.instrument.domain;

import javax.persistence.*;

@javax.persistence.Entity(name = "C")
@Table(name = "C")
public class CEntity {

	@Id
	private long oid;
	private String c1;
	private String c2;
	private String c3;
	private Long c4;

	public String getC1() {
		return c1;
	}

	public void setC1(String c1) {
		this.c1 = c1;
	}

	public String getC2() {
		return c2;
	}

	@Basic(fetch = FetchType.LAZY)
	public void setC2(String c2) {
		this.c2 = c2;
	}

	public String getC3() {
		return c3;
	}

	public void setC3(String c3) {
		this.c3 = c3;
	}

	public Long getC4() {
		return c4;
	}

	public void setC4(Long c4) {
		this.c4 = c4;
	}

	public long getOid() {
		return oid;
	}

	public void setOid(long oid) {
		this.oid = oid;
	}
}
