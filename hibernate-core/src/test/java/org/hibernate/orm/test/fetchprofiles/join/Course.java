/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.fetchprofiles.join;

/**
 * @author Steve Ebersole
 */
public class Course {
	private Integer id;
	private Code code;
	private String name;

	public Course() {
	}

	public Course(Integer id, Code code, String name) {
		this.id = id;
		this.code = code;
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
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
