/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/*
 * Created on 28-Jan-2005
 *
 */
package org.hibernate.test.criteria;


/**
 * @author max
 *
 */
public class StudentDTO {

	private String studentName;
	private String courseDescription;

	public StudentDTO() { }
	
	public String getName() {
		return studentName;
	}
	
	public String getDescription() {
		return courseDescription;
	}
	
}
