/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.userguide.util;
import java.io.Serializable;
import java.util.Date;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Temporal;

/**
 * @author Emmanuel Bernard
 */
@Entity
@IdClass(Sickness.PK.class)
public class Sickness {
	private Date beginTime;
	private String type;
	private String classification;

	@Id
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Id
	public String getClassification() {
		return classification;
	}

	public void setClassification(String classification) {
		this.classification = classification;
	}


	@Temporal(jakarta.persistence.TemporalType.DATE)
	public Date getBeginTime() {
		return beginTime;
	}

	public void setBeginTime(Date beginTime) {
		this.beginTime = beginTime;
	}

	public static class PK implements Serializable {
		private String type;
		private String classification;

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getClassification() {
			return classification;
		}

		public void setClassification(String classification) {
			this.classification = classification;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			PK pk = ( PK ) o;

			if ( classification != null ? !classification.equals( pk.classification ) : pk.classification != null ) {
				return false;
			}
			if ( type != null ? !type.equals( pk.type ) : pk.type != null ) {
				return false;
			}

			return true;
		}

		@Override
		public int hashCode() {
			int result = type != null ? type.hashCode() : 0;
			result = 31 * result + ( classification != null ? classification.hashCode() : 0 );
			return result;
		}
	}
}
