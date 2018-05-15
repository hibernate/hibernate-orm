/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.onetoone.hhh4851;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;

@MappedSuperclass
public class BaseEntity {

	private Long id;
	private Owner owner;
	private Integer version;

	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "owner_id", nullable = false)
	public Owner getOwner() {
		return owner;
	}

	@Version
	public Integer getVersion() {
		return version;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setOwner(Owner owner) {
		this.owner = owner;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}
}
