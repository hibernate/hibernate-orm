/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.inheritance.basic;

import java.sql.Date;
import jakarta.persistence.MappedSuperclass;


/**
 * @author Emmanuel Bernard
 */
@MappedSuperclass
public class Building extends Area {
	public Date getBuiltIn() {
		return builtIn;
	}

	public void setBuiltIn(Date builtIn) {
		this.builtIn = builtIn;
	}

	private Date builtIn;
}
