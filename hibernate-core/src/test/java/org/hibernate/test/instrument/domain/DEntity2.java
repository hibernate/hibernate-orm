package org.hibernate.test.instrument.domain;

import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.sql.Blob;
import java.util.Set;

@javax.persistence.Entity(name = "D2")
@Table(name = "D2")
public class DEntity2 {

	// ****** ID *****************
	@Id
	private long oid;
	private String d;

	// ****** Relations *****************

	@OneToOne( fetch = FetchType.LAZY )
	@LazyToOne( LazyToOneOption.PROXY )
	@JoinColumn( name = "a_oid", unique = true )
//		@LazyGroup("a")
	public AEntity2 a;

	@OneToOne(fetch = FetchType.LAZY)
	@LazyToOne( LazyToOneOption.PROXY )
	@MapsId
//		@LazyGroup("c")
	public CEntity2 c;

	@OneToMany(targetEntity = BEntity2.class)
	public Set<BEntity2> bs;

	@OneToOne(mappedBy = "d", fetch = FetchType.LAZY)
	@LazyToOne(LazyToOneOption.PROXY)
//		@LazyGroup("e")
	private EEntity2 e;

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

	public long getOid() {
		return oid;
	}

	public void setOid(long oid) {
		this.oid = oid;
	}

	public AEntity2 getA() {
		return a;
	}

	public void setA(AEntity2 a) {
		this.a = a;
	}

	public CEntity2 getC() {
		return c;
	}

	public void setC(CEntity2 c) {
		this.c = c;
	}

	public Set<BEntity2> getBs() {
		return bs;
	}

	public void setBs(Set<BEntity2> bs) {
		this.bs = bs;
	}

	public EEntity2 getE() {
		return e;
	}

	public void setE(EEntity2 e) {
		this.e = e;
	}

	public Blob getBlob() {
		return blob;
	}

	public void setBlob(Blob blob) {
		this.blob = blob;
	}
}
