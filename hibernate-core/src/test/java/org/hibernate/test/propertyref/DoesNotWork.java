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
package org.hibernate.test.propertyref;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OrderColumn;
import javax.persistence.Table;

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
