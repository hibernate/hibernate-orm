package org.hibernate.test.orphan.elementcollection;

public class StudentEnrolledClass {
	private EnrollableClass enrolledClass;
	private int classStartTime;
	private EnrolledClassSeat seat;
	
	public EnrollableClass getEnrolledClass() {
		return enrolledClass;
	}
	
	public void setEnrolledClass(EnrollableClass enrolledClass) {
		this.enrolledClass = enrolledClass;
	}
	
	public int getClassStartTime() {
		return classStartTime;
	}
	
	public void setClassStartTime(int classStartTime) {
		this.classStartTime = classStartTime;
	}

	public EnrolledClassSeat getSeat() {
		return seat;
	}

	public void setSeat(EnrolledClassSeat seat) {
		this.seat = seat;
	}
}
