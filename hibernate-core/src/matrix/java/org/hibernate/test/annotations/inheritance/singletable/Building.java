//$Id$
package org.hibernate.test.annotations.inheritance.singletable;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
@DiscriminatorColumn(name = "discriminator_disc")
@DiscriminatorValue("B")
public class Building {
	@Id
	@GeneratedValue
	@Column(name = "discriminator_id")
	private Integer id;
	@Column(name = "discriminator_street")
	private String street;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}
}
