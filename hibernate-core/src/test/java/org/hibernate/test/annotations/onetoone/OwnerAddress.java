//$Id$
package org.hibernate.test.annotations.onetoone;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class OwnerAddress {
	@Id @GeneratedValue(generator = "fk")
	@GenericGenerator(strategy = "foreign", name = "fk", parameters = @Parameter(name="property", value="owner"))
	private Integer id;

	@OneToOne(mappedBy="address")
	private Owner owner;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Owner getOwner() {
		return owner;
	}

	public void setOwner(Owner owner) {
		this.owner = owner;
	}
}
