package org.hibernate.test.ops;
import java.util.Date;

/**
 * todo: describe TimestampedEntity
 *
 * @author Steve Ebersole
 */
public class TimestampedEntity {
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

