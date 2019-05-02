/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy;

import java.sql.Timestamp;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
@MappedSuperclass
public abstract class ModelEntity {

	@Id
	@Column(name="Oid")
	private Long Oid = null;

	@Basic
	@Column(name="CreatedAt")
	private Timestamp CreatedAt = null;

	@Basic
	@Column(name="CreatedBy")
	private String CreatedBy = null;

	@Basic
	@Column(name="VersionNr")
	private short VersionNr = 0;

	public short getVersionNr() {
		return VersionNr;
	}

	public void setVersionNr(short versionNr) {
		this.VersionNr = versionNr;
	}

	public Long getOid() {
		return Oid;
	}

	public void setOid(Long oid) {
		this.Oid = oid;
	}

	public Timestamp getCreatedAt() {
		return CreatedAt;
	}

	public void setCreatedAt(Timestamp createdAt) {
		this.CreatedAt = createdAt;
	}

	public String getCreatedBy() {
		return CreatedBy;
	}

	public void setCreatedBy(String createdBy) {
		this.CreatedBy = createdBy;
	}

}
