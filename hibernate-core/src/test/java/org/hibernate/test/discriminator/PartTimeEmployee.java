//$Id: Employee.java 4373 2004-08-18 09:18:34Z oneovthafew $
package org.hibernate.test.discriminator;
import java.math.BigDecimal;

/**
 * @author Gail Badner
 */
public class PartTimeEmployee extends Employee {
	private String title;
	private BigDecimal salary;
	private Employee manager;
	private int percent;

	public int getPercent() {
		return percent;
	}

	public void setPercent(int percent) {
		this.percent = percent;
	}
}
