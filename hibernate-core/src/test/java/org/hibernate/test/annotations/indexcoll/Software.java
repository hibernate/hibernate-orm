/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.indexcoll;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Software {
	private String name;
	private Map<String, Version> versions = new HashMap<String, Version>();

	@Id
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@OneToMany(mappedBy = "software")
	@MapKey(name = "codeName")
	public Map<String, Version> getVersions() {
		return versions;
	}

	public void setVersions(Map<String, Version> versions) {
		this.versions = versions;
	}

	public void addVersion(Version version) {
		this.versions.put(version.getCodeName(), version);
	}
}
