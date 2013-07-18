/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
