package org.hibernate.test.annotations.immutable;

import java.io.Serializable;
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

}
