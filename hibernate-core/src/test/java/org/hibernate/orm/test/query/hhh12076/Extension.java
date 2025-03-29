/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hhh12076;

import java.util.Date;

public abstract class Extension {
	public static final long serialVersionUID = 1L;

	private Long _id;
	private Date _creationDate;
	private Date _modifiedDate;
	private Integer _version;

	private String _type;

	private Claim _claim;

	public Extension() {
		String[] name = this.getClass().getName().split( "\\." );
		_type = name[name.length - 1];
	}

	public Long getId() {
		return _id;
	}

	protected void setId(Long id) {
		_id = id;
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

	public Integer getVersion() {
		return _version;
	}

	public void setVersion(Integer version) {
		_version = version;
	}

	public Claim getClaim() {
		return _claim;
	}

	public void setClaim(Claim claim) {
		_claim = claim;
	}

	public String getType() {
		return _type;
	}

	public void setType(String type) {
		_type = type;
	}

}
