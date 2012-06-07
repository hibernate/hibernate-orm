//$Id$
package org.hibernate.test.annotations.referencedcolumnname;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.OneToMany;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Luggage implements Serializable {
	private Integer id;
	private String owner;
	private String type;
	private Set<Clothes> hasInside = new HashSet<Clothes>();

	public Luggage() {
	}

	public Luggage(String owner, String type) {
		this.owner = owner;
		this.type = type;
	}

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
	@JoinColumns({
	@JoinColumn(name = "lug_type", referencedColumnName = "type"),
	@JoinColumn(name = "lug_owner", referencedColumnName = "owner")
			})
	public Set<Clothes> getHasInside() {
		return hasInside;
	}

	public void setHasInside(Set<Clothes> hasInside) {
		this.hasInside = hasInside;
	}

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof Luggage ) ) return false;

		final Luggage luggage = (Luggage) o;

		if ( !owner.equals( luggage.owner ) ) return false;
		if ( !type.equals( luggage.type ) ) return false;

		return true;
	}

	public int hashCode() {
		int result;
		result = owner.hashCode();
		result = 29 * result + type.hashCode();
		return result;
	}
}
