//$Id$
package org.hibernate.test.annotations.referencedcolumnname;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class House implements Serializable {
	private Integer id;
	private String address;
	private Postman postman;
	private Set<Inhabitant> hasInhabitants = new HashSet<Inhabitant>();

	@ManyToOne
	@JoinColumn(referencedColumnName = "name")
	public Postman getPostman() {
		return postman;
	}

	public void setPostman(Postman postman) {
		this.postman = postman;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	@ManyToMany
	@JoinTable(joinColumns = @JoinColumn(referencedColumnName = "address"),
			inverseJoinColumns = @JoinColumn(referencedColumnName = "name")
	)
	public Set<Inhabitant> getHasInhabitants() {
		return hasInhabitants;
	}

	public void setHasInhabitants(Set<Inhabitant> hasInhabitants) {
		this.hasInhabitants = hasInhabitants;
	}

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof House ) ) return false;

		final House house = (House) o;

		if ( address != null ? !address.equals( house.address ) : house.address != null ) return false;

		return true;
	}

	public int hashCode() {
		return ( address != null ? address.hashCode() : 0 );
	}
}
