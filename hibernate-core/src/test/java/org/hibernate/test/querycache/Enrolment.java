//$Id: Enrolment.java 6970 2005-05-31 20:24:41Z oneovthafew $
package org.hibernate.test.querycache;
import java.io.Serializable;

/**
 * @author Gavin King
 */
public class Enrolment implements Serializable {
	private Student student;
	private Course course;
	private long studentNumber;
	private String courseCode;
	private short year;
	private short semester;

	public String getCourseCode() {
		return courseCode;
	}
	public void setCourseCode(String courseId) {
		this.courseCode = courseId;
	}
	public long getStudentNumber() {
		return studentNumber;
	}
	public void setStudentNumber(long studentId) {
		this.studentNumber = studentId;
	}
	public Course getCourse() {
		return course;
	}
	public void setCourse(Course course) {
		this.course = course;
	}
	public Student getStudent() {
		return student;
	}
	public void setStudent(Student student) {
		this.student = student;
	}
	public short getSemester() {
		return semester;
	}
	public void setSemester(short semester) {
		this.semester = semester;
	}
	public short getYear() {
		return year;
	}
	public void setYear(short year) {
		this.year = year;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		Enrolment enrolment = ( Enrolment ) o;

		if ( semester != enrolment.semester ) {
			return false;
		}
		if ( studentNumber != enrolment.studentNumber ) {
			return false;
		}
		if ( year != enrolment.year ) {
			return false;
		}
		if ( courseCode != null ? !courseCode.equals( enrolment.courseCode ) : enrolment.courseCode != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result = ( int ) ( studentNumber ^ ( studentNumber >>> 32 ) );
		result = 31 * result + ( courseCode != null ? courseCode.hashCode() : 0 );
		result = 31 * result + ( int ) year;
		result = 31 * result + ( int ) semester;
		return result;
	}
}
