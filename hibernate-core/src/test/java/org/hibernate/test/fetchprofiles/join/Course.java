/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
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
