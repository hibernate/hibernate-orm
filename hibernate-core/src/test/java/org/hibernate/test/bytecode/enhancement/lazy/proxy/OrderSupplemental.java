/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity(name = "OrderSupplemental")
@Table(name = "order_supp")
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
	@Column(name = "oid")
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
