/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.hhh12076;

import java.util.Date;

public abstract class SettlementExtension {

	private Long _id;
	private Integer _version;
	private Date _creationDate;
	private Date _modifiedDate;

	private Integer _orderIndex;
	private Settlement _settlement;

	public SettlementExtension() {
	}

	public Claim getClaim() {
		return _settlement.getClaim();
	}

	public Long getId() {
		return _id;
	}

	protected void setId(Long id) {
		_id = id;
	}

	public Integer getVersion() {
		return _version;
	}

	public void setVersion(Integer version) {
		_version = version;
	}

	public Date getCreationDate() {
		return _creationDate;
	}

	public void setCreationDate(Date creationDate) {
		_creationDate = creationDate;
	}

	public Date getModifiedDate() {
		return _modifiedDate;
	}

	public void setModifiedDate(Date modifiedDate) {
		_modifiedDate = modifiedDate;
	}

	public Settlement getSettlement() {
		return _settlement;
	}

	public void setSettlement(Settlement settlement) {
		_settlement = settlement;
	}

	public Integer getOrderIndex() {
		return _orderIndex;
	}

	public void setOrderIndex(Integer orderIndex) {
		_orderIndex = orderIndex;
	}

}
