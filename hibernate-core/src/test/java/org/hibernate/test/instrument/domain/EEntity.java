package org.hibernate.test.instrument.domain;

import javax.persistence.*;

/**
 * Created by sebersole on 4/16/19.
 */
@javax.persistence.Entity(name = "E")
@Table(name = "E")
public class EEntity {
	@Id
	private long oid;
	private String e1;
	private String e2;

	@OneToOne(fetch = FetchType.LAZY)
	private DEntity d;

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

	public DEntity getD() {
		return d;
	}

	public void setD(DEntity d) {
		this.d = d;
	}
}
