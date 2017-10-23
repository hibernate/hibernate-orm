/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.orphan.one2one.fk.composite;
import java.io.Serializable;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class EmployeeInfo {
	public static class Id implements Serializable {
		private Long companyId;
		private Long personId;

		public Id() {
		}

		public Id(Long companyId, Long personId) {
			this.companyId = companyId;
			this.personId = personId;
		}

		public Long getCompanyId() {
			return companyId;
		}

		public void setCompanyId(Long companyId) {
			this.companyId = companyId;
		}

		public Long getPersonId() {
			return personId;
		}

		public void setPersonId(Long personId) {
			this.personId = personId;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			Id id = (Id) o;

			return companyId.equals( id.companyId )
					&& personId.equals( id.personId );

		}

		@Override
		public int hashCode() {
			int result = companyId.hashCode();
			result = 31 * result + personId.hashCode();
			return result;
		}
	}

	private Id id;

	public EmployeeInfo() {
	}

	public EmployeeInfo(Long companyId, Long personId) {
		this( new Id( companyId, personId ) );
	}

	public EmployeeInfo(Id id) {
		this.id = id;
	}

	public Id getId() {
		return id;
	}

	public void setId(Id id) {
		this.id = id;
	}
}
