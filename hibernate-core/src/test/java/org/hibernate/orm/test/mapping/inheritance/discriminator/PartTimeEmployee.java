/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.inheritance.discriminator;
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
