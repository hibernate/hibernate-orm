//$Id$
package org.hibernate.test.annotations.strategy;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"start.country", "start.city"}),
		indexes = @Index(name="storm_name_idx", columnList = "stormName"))
public class Storm {
	private Integer id;
	private Location start;
	private Location end;
	private String stormName;

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Embedded
	public Location getStart() {
		return start;
	}

	public void setStart(Location start) {
		this.start = start;
	}

	@Embedded
	public Location getEnd() {
		return end;
	}

	public void setEnd(Location end) {
		this.end = end;
	}

	@Column(unique = true)
	public String getStormName() {
		return stormName;
	}

	public void setStormName(String name) {
		this.stormName = name;
	}
}
