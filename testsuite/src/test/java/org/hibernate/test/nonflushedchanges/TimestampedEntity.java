package org.hibernate.test.nonflushedchanges;

import java.io.Serializable;
import java.util.Date;

/**
 * todo: describe TimestampedEntity
 *
 * @author Steve Ebersole, Gail Badner (adapted this from "ops" tests version)
 */
public class TimestampedEntity implements Serializable {
	private String id;
	private String name;
	private Date timestamp;

	public TimestampedEntity() {
	}

	public TimestampedEntity(String id, String name) {
		this.id = id;
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
}

