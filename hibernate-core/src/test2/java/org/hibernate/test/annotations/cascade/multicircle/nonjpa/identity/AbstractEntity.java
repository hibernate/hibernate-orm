/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.cascade.multicircle.nonjpa.identity;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AbstractEntity implements Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Basic
	@Column(unique = true, updatable = false, length = 36, columnDefinition = "char(36)")
	private String uuid;
	@Column(updatable = false)
	private Date created;

	public AbstractEntity() {
		super();
		uuid = UUID.randomUUID().toString();
		created = new Date();
	}

	public Long getId() {
		return id;
	}

	public String getUuid() {
		return uuid;
	}

	public Date getCreated() {
		return created;
	}

	@Override
	public int hashCode() {
		return uuid == null ? 0 : uuid.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof AbstractEntity ))
			return false;
		final AbstractEntity other = (AbstractEntity) obj;
		if (uuid == null) {
			if (other.uuid != null)
				return false;
		} else if (!uuid.equals(other.uuid))
			return false;
		return true;
	}

	public String toString() {
		if (id != null) {
			return "id: '" + id + "' uuid: '" + uuid + "'";
		} else {
			return "id: 'transient entity' " + " uuid: '" + uuid + "'";
		}
	}
}
