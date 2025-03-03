/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;
import java.util.Date;

/**
 * Implementation of TimestampVersioned.
 *
 * @author Steve Ebersole
 */
public class TimestampVersioned {
	private Long id;
	private Date version;
	private String name;

	public TimestampVersioned() {
	}

	public TimestampVersioned(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public Date getVersion() {
		return version;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
