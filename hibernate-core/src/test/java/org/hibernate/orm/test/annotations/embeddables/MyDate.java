/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.embeddables;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Chris Pheby
 */
public class MyDate implements Serializable {

	private static final long serialVersionUID = -416056386419355705L;

	private Date date;

	public MyDate() {
	}

	public MyDate(Date date) {
		this.date = date;
	}

	public Date getDate() {
		return date;
	}
}
