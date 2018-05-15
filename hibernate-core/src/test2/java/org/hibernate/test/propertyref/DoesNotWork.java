/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.propertyref;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.IndexColumn;
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
