package org.hibernate.test.annotations.inheritance;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

/**
 * @author Emmanuel Bernard
 */
@Entity()
@Inheritance(
		strategy = InheritanceType.JOINED
)
public class Vegetable {
	private VegetablePk id;
	private long priceInCent;

	@Id
	public VegetablePk getId() {
		return id;
	}

	public void setId(VegetablePk id) {
		this.id = id;
	}

	public long getPriceInCent() {
		return priceInCent;
	}

	public void setPriceInCent(long priceInCent) {
		this.priceInCent = priceInCent;
	}

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof Vegetable ) ) return false;

		final Vegetable vegetable = (Vegetable) o;

		if ( !id.equals( vegetable.id ) ) return false;

		return true;
	}

	public int hashCode() {
		return id.hashCode();
	}
}
