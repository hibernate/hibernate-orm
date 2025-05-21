/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking.options;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Basic;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity
@Table(name = "details")
@SecondaryTable(name = "supplementals", pkJoinColumns = @PrimaryKeyJoinColumn(name = "detail_fk"))
public class Detail {
	@Id
	private Integer id;
	@Basic
	private String name;
	@Column(name = "txt", table = "supplementals")
	private String data;

	protected Detail() {
		// for Hibernate use
	}

	public Detail(Integer id, String name, String data) {
		this.id = id;
		this.name = name;
		this.data = data;
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}
}
