//$Id$
package org.hibernate.test.annotations.onetoone;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Owner {
	@Id @GeneratedValue private Integer id;

	@OneToOne(cascade = CascadeType.ALL) @PrimaryKeyJoinColumn private OwnerAddress address;

	public OwnerAddress getAddress() {
		return address;
	}

	public void setAddress(OwnerAddress address) {
		this.address = address;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}
}
