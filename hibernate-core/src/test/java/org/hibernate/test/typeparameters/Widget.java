/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.typeparameters;


/**
 * @author Michael Gloegl
 */
public class Widget {

	private int valueOne = 1;
	private int valueTwo = 2;
	private int valueThree = -1;
	private int valueFour = -5;
	private Integer id;
	private String string;

	public int getValueOne() {
		return valueOne;
	}

	public void setValueOne(int valueOne) {
		this.valueOne = valueOne;
	}

	public int getValueThree() {
		return valueThree;
	}

	public void setValueThree(int valueThree) {
		this.valueThree = valueThree;
	}

	public int getValueTwo() {
		return valueTwo;
	}

	public void setValueTwo(int valueTwo) {
		this.valueTwo = valueTwo;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getString() {
		return string;
	}

	public void setString(String string) {
		this.string = string;
	}

	public int getValueFour() {
		return valueFour;
	}

	public void setValueFour(int valueFour) {
		this.valueFour = valueFour;
	}
}
