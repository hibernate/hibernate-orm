package org.hibernate.test.criteria;


/**
 * @author Gail Badner
 */
public class CourseMeeting {
	private CourseMeetingId id;
	private Course course;

	public CourseMeeting() {}

	public CourseMeeting(Course course, String day, int period, String location) {
		this.id = new CourseMeetingId( course, day, period, location );
		this.course = course;
	}

	public CourseMeetingId getId() {
		return id;
	}
	public void setId(CourseMeetingId id) {
		this.id = id;
	}
	public  Course getCourse() {
		return course;
	}
	public void setCourse(Course course) {
		this.course = course;
	}
}
