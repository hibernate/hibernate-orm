//$Id$
package org.hibernate.test.annotations;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

/**
 * Used to test that annotated classes can have one-to-many
 * relationships with hbm loaded classes.
 */
@Entity()
public class Port {
	private Long id;
	private Set<Boat> boats;

	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	public void setId(Long long1) {
		id = long1;
	}

	@OneToMany
	public Set<Boat> getBoats() {
		return boats;
	}

	public void setBoats(Set<Boat> boats) {
		this.boats = boats;
	}
}
