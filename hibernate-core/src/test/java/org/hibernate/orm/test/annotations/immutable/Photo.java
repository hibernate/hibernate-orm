/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.immutable;

import java.io.Serializable;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 *
 * @author soldierkam
 */
@Entity
@SuppressWarnings("serial")
public class Photo implements Serializable {

	private Integer id;

	private String name;

	private Exif metadata;

	private Caption caption;

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setId(Integer integer) {
		id = integer;
	}

	public void setName(String string) {
		name = string;
	}

	public Exif getMetadata() {
		return metadata;
	}

	public void setMetadata(Exif metadata) {
		this.metadata = metadata;
	}

	@Convert(converter = CaptionConverter.class)
	public Caption getCaption() {
		return caption;
	}

	public void setCaption(Caption caption) {
		this.caption = caption;
	}
}
