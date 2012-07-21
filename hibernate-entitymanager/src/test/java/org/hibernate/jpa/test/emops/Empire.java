//$Id$
package org.hibernate.jpa.test.emops;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Empire {
	@Id
	@GeneratedValue
	private Long id;

	@OneToMany(cascade= CascadeType.ALL )
	private Set<Colony> colonies = new HashSet<Colony>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Set<Colony> getColonies() {
		return colonies;
	}

	public void setColonies(Set<Colony> colonies) {
		this.colonies = colonies;
	}
}
