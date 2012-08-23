/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.collectionalias;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * @author Dave Stephan
 */
@Embeddable
public class TableBId implements Serializable
{
		private static final long serialVersionUID = 1L;

		// Fields

		private Integer firstId;

		private String secondId;

		private String thirdId;

		// Constructors

		/** default constructor */
		public TableBId() {
		}

		/** full constructor */
		public TableBId(Integer firstId, String secondId, String thirdId) {
			this.firstId = firstId;
			this.secondId = secondId;
			this.thirdId = thirdId;
		}

		// Property accessors

		@Column(name = "idcolumn", nullable = false)
		public Integer getFirstId() {
			return this.firstId;
		}

		public void setFirstId(Integer firstId) {
			this.firstId = firstId;
		}

		@Column(name = "idcolumn_second", nullable = false, length = 50)
		public String getSecondId() {
			return this.secondId;
		}

		public void setSecondId(String secondId) {
			this.secondId = secondId;
		}

	@Column(name = "thirdcolumn", nullable = false, length = 50)
		public String getThirdId() {
			return this.thirdId;
		}

		public void setThirdId(String thirdId) {
			this.thirdId = thirdId;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((firstId == null) ? 0 : firstId.hashCode());
			result = prime * result + ((secondId == null) ? 0 : secondId.hashCode());
			result = prime * result + ((thirdId == null) ? 0 : thirdId.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TableBId other = (TableBId) obj;
			if (firstId == null)
			{
				if (other.firstId != null)
					return false;
			}
			else if (!firstId.equals(other.firstId))
				return false;
			if (secondId == null)
			{
				if (other.secondId != null)
					return false;
			}
			else if (!secondId.equals(other.secondId))
				return false;
			if (thirdId == null)
			{
				if (other.thirdId != null)
					return false;
			}
			else if (!thirdId.equals(other.thirdId))
				return false;
			return true;
		}
}
