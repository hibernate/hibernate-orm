/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.various;

import java.util.Date;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class DBTimestamped {
	@Id
	@GeneratedValue
	private int id;

	@Version
	@UpdateTimestamp(source = SourceType.DB)
	private Date lastUpdate;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Date getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(Date lastUpdate) {
		this.lastUpdate = lastUpdate;
	}
}
