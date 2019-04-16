package org.hibernate.test.instrument.domain;

import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@javax.persistence.Entity(name = "E2")
@Table(name = "E2")
public class EEntity2 {
	@Id
	private long oid;
	private String e1;
	private String e2;

	@OneToOne(fetch = FetchType.LAZY)
	private DEntity2 d;

	public long getOid() {
		return oid;
	}

	public void setOid(long oid) {
		this.oid = oid;
	}

	public String getE1() {
		return e1;
	}

	public void setE1(String e1) {
		this.e1 = e1;
	}

	public String getE2() {
		return e2;
	}

	public void setE2(String e2) {
		this.e2 = e2;
	}

	public DEntity2 getD() {
		return d;
	}

	public void setD(DEntity2 d) {
		this.d = d;
	}
}
