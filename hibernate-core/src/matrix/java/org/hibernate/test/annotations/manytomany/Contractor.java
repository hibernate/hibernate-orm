//$Id: Employee.java 14736 2008-06-04 14:23:42Z hardy.ferentschik $
package org.hibernate.test.annotations.manytomany;
import java.io.Serializable;
import javax.persistence.Entity;

/**
 * Employee in an Employer-Employee relationship
 *
 * @author Emmanuel Bernard
 */
@Entity
@SuppressWarnings("serial")
public class Contractor extends Employee implements Serializable {

	private float hourlyRate;

	public float getHourlyRate() {
		return hourlyRate;
	}

	public void setHourlyRate(float hourlyRate) {
		this.hourlyRate = hourlyRate;
	}
}
