//$Id: Student.java 9116 2006-01-23 21:21:01Z steveebersole $
package org.hibernate.test.querycache;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Gavin King
 */
public class Student {
	private long studentNumber;
	private PersonName name;
	private Course preferredCourse;
	private Set enrolments = new HashSet();
	private Map addresses = new HashMap();
	private List secretCodes = new ArrayList();

	public Student() {}

	public Student(long studentNumber, PersonName name) {
		this.studentNumber = studentNumber;
		this.name = name;
	}

	public PersonName getName() {
		return name;
	}

	public void setName(PersonName name) {
		this.name = name;
	}

	public long getStudentNumber() {
		return studentNumber;
	}

	public void setStudentNumber(long studentNumber) {
		this.studentNumber = studentNumber;
	}

	public Map getAddresses() {
		return addresses;
	}

	public void setAddresses(Map addresses) {
		this.addresses = addresses;
	}

	public Course getPreferredCourse() {
		return preferredCourse;
	}

	public void setPreferredCourse(Course preferredCourse) {
		this.preferredCourse = preferredCourse;
	}

	public Set getEnrolments() {
		return enrolments;
	}

	public void setEnrolments(Set employments) {
		this.enrolments = employments;
	}

	public List getSecretCodes() {
		return secretCodes;
	}

	public void setSecretCodes(List secretCodes) {
		this.secretCodes = secretCodes;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || ! ( o instanceof Student ) ) {
			return false;
		}

		Student student = ( Student ) o;

		if ( studentNumber != student.getStudentNumber() ) {
			return false;
		}
		if ( name != null ? !name.equals( student.getName() ) : student.getName() != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result = ( int ) ( studentNumber ^ ( studentNumber >>> 32 ) );
		result = 31 * result + ( name != null ? name.hashCode() : 0 );
		return result;
	}
}
