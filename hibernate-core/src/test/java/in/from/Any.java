/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package in.from;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Andrea Boriero
 */
@Entity
@Table(name = "`ANY`")
public class Any {
	@Id
	@GeneratedValue
	private long id;

	private String prop;

	public Any() {
	}

	public Any( String prop) {
		this.prop = prop;
	}

	public long getId() {
		return id;
	}

	public String getProp() {
		return prop;
	}

	public void setProp(String prop) {
		this.prop = prop;
	}
}
