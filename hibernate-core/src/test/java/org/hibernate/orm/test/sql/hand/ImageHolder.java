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
@Table(name = "IMAGEHOLDER")
@SQLInsert(sql = "INSERT INTO IMAGEHOLDER (PHOTO, ID) VALUES (?, ?)")
@SQLUpdate(sql = "UPDATE IMAGEHOLDER SET PHOTO=? WHERE ID=?")
@SQLDelete(sql = "DELETE FROM IMAGEHOLDER WHERE ID=?")
@SQLSelect(sql = "SELECT ID, PHOTO FROM IMAGEHOLDER WHERE ID=?")
public class ImageHolder {
	@Id
	@GenericGenerator(type = IncrementGenerator.class)
	@Column(name = "ID")
	private Long id;

	@Lob
	@Column(name = "PHOTO", length = 15000)
	private byte[] photo;

	public ImageHolder(byte[] photo) {
		this.photo = photo;
	}

	public ImageHolder() {
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
	 * @return Returns the photo.
	 */
	public byte[] getPhoto() {
		return photo;
	}

	/**
	 * @param photo The photo to set.
	 */
	public void setPhoto(byte[] photo) {
		this.photo = photo;
	}
}
