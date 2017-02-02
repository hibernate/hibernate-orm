/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.immutable;

import java.io.Serializable;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

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
