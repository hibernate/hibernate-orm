package org.hibernate.test.instrument.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity( name = "OrderSupplemental" )
@Table( name = "order_supp" )
public class OrderSupplemental {
	private Integer oid;
	private Integer receivablesId;

	public OrderSupplemental() {
	}

	public OrderSupplemental(Integer oid, Integer receivablesId) {
		this.oid = oid;
		this.receivablesId = receivablesId;
	}

	@Id
	@Column( name = "oid" )
	public Integer getOid() {
		return oid;
	}

	public void setOid(Integer oid) {
		this.oid = oid;
	}

	public Integer getReceivablesId() {
		return receivablesId;
	}

	public void setReceivablesId(Integer receivablesId) {
		this.receivablesId = receivablesId;
	}
}
