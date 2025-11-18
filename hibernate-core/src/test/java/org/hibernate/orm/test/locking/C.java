/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.GenericGenerator;

/**
 * @author Steve Ebersole
 */
@Entity
@Table( name = "T_LOCK_C" )
public class C {
	private Long id;
	private String value;
	private int version;

	public C() {
	}

	public C(String value) {
		this.value = value;
	}

	@Id
	@GeneratedValue( generator = "increment" )
	@GenericGenerator( name = "increment", strategy = "increment" )
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name="c_value")
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Version
	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}
}
