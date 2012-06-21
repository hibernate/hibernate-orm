/*
 * Created on 28-Jan-2005
 *
 */
package org.hibernate.test.readonly;


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
