//$Id$
package org.hibernate.test.annotations.override;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

/**
 * @author Emmanuel Bernard
 */
@MappedSuperclass
public class Move {
	private int id;
	private Location from;
	private Location to;

	@ManyToOne
	public Location getFrom() {
		return from;
	}

	public void setFrom(Location from) {
		this.from = from;
	}

	@Id
	@GeneratedValue
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@ManyToOne
	@JoinColumn(name = "to", nullable = true)
	public Location getTo() {
		return to;
	}

	public void setTo(Location to) {
		this.to = to;
	}
}
