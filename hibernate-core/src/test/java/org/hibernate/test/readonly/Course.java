//$Id: Course.java 5686 2005-02-12 07:27:32Z steveebersole $
package org.hibernate.test.readonly;


/**
 * @author Gavin King
 */
public class Course {
	private String courseCode;
	private String description;
	public String getCourseCode() {
		return courseCode;
	}
	public void setCourseCode(String courseCode) {
		this.courseCode = courseCode;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
}
