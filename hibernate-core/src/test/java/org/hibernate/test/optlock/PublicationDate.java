/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: PublicationDate.java 7556 2005-07-19 23:22:27Z oneovthafew $
package org.hibernate.test.optlock;


public class PublicationDate {
	private int year;
	private Integer month;
	
	public PublicationDate(int year) {
		this.year = year;
	}
	
	PublicationDate() {}
	
	public Integer getMonth() {
		return month;
	}
	public void setMonth(Integer month) {
		this.month = month;
	}
	public int getYear() {
		return year;
	}
	public void setYear(int year) {
		this.year = year;
	}
}
