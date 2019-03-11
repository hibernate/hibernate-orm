/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.collection.dereferenced;

import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Version;

/**
 * @author Gail Badner
 */
@Entity
public class VersionedNoCascadeOne {
	@Id
	@GeneratedValue
	private long id;

	@OneToMany
	@JoinColumn
	private Set<Many> manies;

	@Version
	private long version;

	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}

	public Set<Many> getManies() {
		return manies;
	}
	public void setManies(Set<Many> manies) {
		this.manies = manies;
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}
}
