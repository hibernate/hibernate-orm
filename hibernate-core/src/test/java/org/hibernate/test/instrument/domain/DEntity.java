package org.hibernate.test.instrument.domain;

import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

import javax.persistence.*;
import java.sql.Blob;
import java.util.Set;

@javax.persistence.Entity(name = "D")
@Table(name = "D")
public class DEntity {

	// ****** ID *****************
	@Id
	private long oid;
	private String d;
	// ****** Relations *****************
	@OneToOne(fetch = FetchType.LAZY)
	@LazyToOne(LazyToOneOption.NO_PROXY)
//		@LazyGroup("a")
	public AEntity a;

	@OneToOne(fetch = FetchType.LAZY)
	@LazyToOne(LazyToOneOption.NO_PROXY)
//		@LazyGroup("c")
	public CEntity c;
	@OneToMany(targetEntity = BEntity.class)
	public Set<BEntity> bs;

	@OneToOne(mappedBy = "d", fetch = FetchType.LAZY)
	@LazyToOne(LazyToOneOption.NO_PROXY)
//		@LazyGroup("e")
	private EEntity e;

	@Lob
	@Basic(fetch = FetchType.LAZY)
//		@LazyGroup("blob")
	private Blob blob;

	public String getD() {
		return d;
	}

	public void setD(String d) {
		this.d = d;
	}


	// ****** ID *****************
	public long getOid() {
		return oid;
	}

	public void setOid(long oid) {
		this.oid = oid;
	}


	public AEntity getA() {
		return a;
	}

	public void setA(AEntity a) {
		this.a = a;
	}

	public Set<BEntity> getBs() {
		return bs;
	}

	public void setBs(Set<BEntity> bs) {
		this.bs = bs;
	}

	public CEntity getC() {
		return c;
	}

	public void setC(CEntity c) {
		this.c = c;
	}

	public Blob getBlob() {
		return blob;
	}

	public void setBlob(Blob blob) {
		this.blob = blob;
	}

	public EEntity getE() {
		return e;
	}

	public void setE(EEntity e) {
		this.e = e;
	}
}
