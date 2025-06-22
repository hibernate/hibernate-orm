/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.hand;


/**
 * @author Gail Badner
 */
public class ImageHolder {
	private Long id;
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
