//$Id$
package org.hibernate.test.annotations;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

/**
 * International passport
 *
 * @author Emmanuel Bernard
 */
@Entity
public class Passport implements Serializable {

	private Long id;
	private String number;
	private Customer owner;

	@Id
	public Long getId() {
		return id;
	}

	@Column(name = "passport_number")
	public String getNumber() {
		return number;
	}

	@OneToOne(mappedBy = "passport")
	public Customer getOwner() {
		return owner;
	}

	public void setId(Long long1) {
		id = long1;
	}

	public void setNumber(String string) {
		number = string;
	}

	public void setOwner(Customer customer) {
		owner = customer;
	}

}
