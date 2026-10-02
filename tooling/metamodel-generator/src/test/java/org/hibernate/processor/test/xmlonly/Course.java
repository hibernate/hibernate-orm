package org.hibernate.processor.test.xmlonly;

import java.util.Set;

public class Course {
	private Long id;
	private String name;
	private Set<Teacher> qualifiedTeachers;
}
