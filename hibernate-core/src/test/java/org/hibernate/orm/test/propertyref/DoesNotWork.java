/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.propertyref;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.ListIndexBase;

/**
 * @author Steve Ebersole
 */
@Entity
@Table(name = "vgras007_v031")
public class DoesNotWork implements Serializable {

	private static final long serialVersionUID = 1L;

	@EmbeddedId
	private DoesNotWorkPk doesNotWorkPk;

	@Column(name = "production_credits_tid", insertable = false, updatable = false)
	private Long globAdditInfoTid;

	@ElementCollection
	@CollectionTable(
			name = "vgras029_v031",
			joinColumns = @JoinColumn(name = "text_id", referencedColumnName = "production_credits_tid")
	)
	@Column(name = "text_part", insertable = false, updatable = false)
	@OrderColumn( name = "seq_no" )
	@ListIndexBase(1)
	private List<String> globalNotes = new ArrayList<String>();

	public DoesNotWork() {
	}

	public DoesNotWork(DoesNotWorkPk doesNotWorkPk) {
		this.doesNotWorkPk = doesNotWorkPk;
	}

	public DoesNotWorkPk getDoesNotWorkPk() {
		return doesNotWorkPk;
	}

	public void setDoesNotWorkPk(DoesNotWorkPk doesNotWorkPk) {
		this.doesNotWorkPk = doesNotWorkPk;
	}

	public List<String> getGlobalNotes() {
		return globalNotes;
	}

	public void setGlobalNotes(List<String> globalNotes) {
		this.globalNotes = globalNotes;
	}

	public Long getGlobAdditInfoTid() {
		return globAdditInfoTid;
	}

	public void setGlobAdditInfoTid(Long globAdditInfoTid) {
		this.globAdditInfoTid = globAdditInfoTid;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((doesNotWorkPk == null) ? 0 : doesNotWorkPk.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( !(obj instanceof DoesNotWork) ) {
			return false;
		}
		DoesNotWork other = (DoesNotWork) obj;
		if ( doesNotWorkPk == null ) {
			if ( other.doesNotWorkPk != null ) {
				return false;
			}
		}
		else if ( !doesNotWorkPk.equals( other.doesNotWorkPk ) ) {
			return false;
		}
		return true;
	}

}
