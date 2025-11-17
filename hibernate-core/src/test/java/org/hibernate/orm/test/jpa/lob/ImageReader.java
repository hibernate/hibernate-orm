/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.lob;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.Clob;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class ImageReader implements Serializable {

	private long id;
	private Blob image;
	private Clob text;

	@Id
	@GeneratedValue
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Lob
	@Column(name = "bin_img")
	public Blob getImage() {
		return image;
	}

	public void setImage(Blob image) {
		this.image = image;
	}

	@Lob
	@Column(name = "img_text")
	public Clob getText() {
		return text;
	}

	public void setText(Clob text) {
		this.text = text;
	}
}
