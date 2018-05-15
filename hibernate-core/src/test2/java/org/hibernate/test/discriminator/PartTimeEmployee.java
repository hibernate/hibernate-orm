/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

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
