//$Id: UniversityCourse.java 6913 2005-05-25 17:37:51Z oneovthafew $
package org.hibernate.test.ecid;


/**
 * @author Gavin King
 */
public class UniversityCourse extends Course {
	
	private int semester;

	UniversityCourse() {}

	public UniversityCourse(String courseCode, String org, String description, int semester) {
		super( courseCode, org, description );
		this.semester = semester;
	}

	public int getSemester() {
		return semester;
	}

}
