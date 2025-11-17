/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collectionalias;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

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
