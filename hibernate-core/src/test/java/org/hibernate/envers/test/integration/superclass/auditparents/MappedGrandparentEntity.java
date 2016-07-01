/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.superclass.auditparents;

import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.hibernate.envers.NotAudited;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@MappedSuperclass
public class MappedGrandparentEntity {
	@Id
	private Long id;

	private String grandparent;

	@NotAudited
	private String notAudited;

	public MappedGrandparentEntity(Long id, String grandparent, String notAudited) {
		this.id = id;
		this.grandparent = grandparent;
		this.notAudited = notAudited;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof MappedGrandparentEntity) ) {
			return false;
		}

		MappedGrandparentEntity that = (MappedGrandparentEntity) o;

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}
		if ( grandparent != null ? !grandparent.equals( that.grandparent ) : that.grandparent != null ) {
			return false;
		}
		if ( notAudited != null ? !notAudited.equals( that.notAudited ) : that.notAudited != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result = (id != null ? id.hashCode() : 0);
		result = 31 * result + (grandparent != null ? grandparent.hashCode() : 0);
		result = 31 * result + (notAudited != null ? notAudited.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "MappedGrandparentEntity(id = " + id + ", grandparent = " + grandparent + ", notAudited = " + notAudited + ")";
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getGrandparent() {
		return grandparent;
	}

	public void setGrandparent(String grandparent) {
		this.grandparent = grandparent;
	}

	public String getNotAudited() {
		return notAudited;
	}

	public void setNotAudited(String notAudited) {
		this.notAudited = notAudited;
	}
}
