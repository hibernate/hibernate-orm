//$Id$
package org.hibernate.test.annotations.onetoone;
import java.io.Serializable;
import javax.persistence.Embeddable;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
public class ComputerPk implements Serializable {
	private String brand;
	private String model;

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof ComputerPk ) ) return false;

		final ComputerPk computerPk = (ComputerPk) o;

		if ( !brand.equals( computerPk.brand ) ) return false;
		if ( !model.equals( computerPk.model ) ) return false;

		return true;
	}

	public int hashCode() {
		int result;
		result = brand.hashCode();
		result = 29 * result + model.hashCode();
		return result;
	}

	public String getBrand() {
		return brand;
	}

	public void setBrand(String brand) {
		this.brand = brand;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}
}
