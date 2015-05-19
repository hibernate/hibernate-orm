/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.fetchprofiles.join;


/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class Course {
	private Long id;
	private Code code;
	private String name;

	public Course() {
	}

	public Course(Code code, String name) {
		this.code = code;
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Code getCode() {
		return code;
	}

	public void setCode(Code code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	public static class Code {
		private Department department;
		private int number;

		public Code() {
		}

		public Code(Department department, int number) {
			this.department = department;
			this.number = number;
		}

		public Department getDepartment() {
			return department;
		}

		public void setDepartment(Department department) {
			this.department = department;
		}

		public int getNumber() {
			return number;
		}

		public void setNumber(int number) {
			this.number = number;
		}

		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( !( o instanceof Code ) ) {
				return false;
			}

			Code code = ( Code ) o;

			if ( number != code.number ) {
				return false;
			}
			if ( !department.equals( code.department ) ) {
				return false;
			}

			return true;
		}

		public int hashCode() {
			int result;
			result = department.hashCode();
			result = 31 * result + number;
			return result;
		}
	}
}
