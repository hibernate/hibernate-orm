//$Id$
package org.hibernate.test.annotations.referencedcolumnname;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Inhabitant implements Serializable {
	private Integer id;
	private String name;
	private Set<House> livesIn = new HashSet<House>();

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@ManyToMany(mappedBy = "hasInhabitants")
	public Set<House> getLivesIn() {
		return livesIn;
	}

	public void setLivesIn(Set<House> livesIn) {
		this.livesIn = livesIn;
	}

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof Inhabitant ) ) return false;

		final Inhabitant inhabitant = (Inhabitant) o;

		if ( name != null ? !name.equals( inhabitant.name ) : inhabitant.name != null ) return false;

		return true;
	}

	public int hashCode() {
		return ( name != null ? name.hashCode() : 0 );
	}
}
