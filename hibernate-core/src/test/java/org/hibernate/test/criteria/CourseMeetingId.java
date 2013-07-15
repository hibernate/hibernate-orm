package org.hibernate.test.criteria;
import java.io.Serializable;

/**
 * @author Gail Badner
 */
public class CourseMeetingId implements Serializable {
	private String courseCode;
	private String day;
	private int period;
	private String location;

	public CourseMeetingId() {}

	public CourseMeetingId(Course course, String day, int period, String location) {
		this.courseCode = course.getCourseCode();
		this.day = day;
		this.period = period;
		this.location = location;
	}

	public String getCourseCode() {
		return courseCode;
	}
	public void setCourseCode(String courseCode) {
		this.courseCode = courseCode;
	}
	public String getDay() {
		return day;
	}
	public void setDay(String day) {
		this.day = day;
	}
	public int getPeriod() {
		return period;
	}
	public void setPeriod(int period) {
		this.period = period;
	}
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
}
