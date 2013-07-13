/*
 * Created on 28-Jan-2005
 *
 */
package org.hibernate.test.querycache;


/**
 * @author max
 *
 */
public class StudentDTO {

	private PersonName studentName;
	private String courseDescription;

	public StudentDTO() { }

	public StudentDTO(PersonName name) {
		this.studentName = name;
	}

	public StudentDTO(PersonName name, String description) {
		this.studentName = name;
		this.courseDescription = description;
	}

	public PersonName getName() {
		return studentName;
	}

	public String getDescription() {
		return courseDescription;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		StudentDTO that = ( StudentDTO ) o;

		if ( courseDescription != null ? !courseDescription.equals( that.courseDescription ) : that.courseDescription != null ) {
			return false;
		}
		if ( studentName != null ? !studentName.equals( that.studentName ) : that.studentName != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result = studentName != null ? studentName.hashCode() : 0;
		result = 31 * result + ( courseDescription != null ? courseDescription.hashCode() : 0 );
		return result;
	}
}
