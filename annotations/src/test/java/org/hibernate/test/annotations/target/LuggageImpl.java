//$Id$
package org.hibernate.test.annotations.target;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.Embedded;

import org.hibernate.annotations.Target;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class LuggageImpl implements Luggage {
	private Long id;
	private double height;
	private double width;
	private Owner owner;

	@Embedded
	@Target(OwnerImpl.class)
	public Owner getOwner() {
		return owner;
	}

	public void setOwner(Owner owner) {
		this.owner = owner;
	}

	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public double getHeight() {
		return height;
	}

	public void setHeight(double height) {
		this.height = height;
	}

	public double getWidth() {
		return width;
	}

	public void setWidth(double width) {
		this.width = width;
	}
}
