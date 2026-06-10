/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.hand;


import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLSelect;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.id.IncrementGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * @author Gail Badner
 */
@Entity
@Table(name = "TEXTHOLDER")
@SQLInsert(sql = "INSERT INTO TEXTHOLDER (DESCRIPTION, ID) VALUES (?, ?)")
@SQLUpdate(sql = "UPDATE TEXTHOLDER SET DESCRIPTION=? WHERE ID=?")
@SQLDelete(sql = "DELETE FROM TEXTHOLDER WHERE ID=?")
@SQLSelect(sql = "SELECT ID, DESCRIPTION FROM TEXTHOLDER WHERE ID=?")
public class TextHolder {
	@Id
	@GenericGenerator(type = IncrementGenerator.class)
	@Column(name = "ID")
	private Long id;

	@Lob
	@Column(name = "DESCRIPTION", length = 15000)
	private String description;

	public TextHolder(String description) {
		this.description = description;
	}

	public TextHolder() {
	}

	/**
	 * @return Returns the id.
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id The id to set.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return Returns the description.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description The description to set.
	 */
	public void setDescription(String description) {
		this.description = description;
	}
}
