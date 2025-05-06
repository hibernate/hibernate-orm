/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.indexcoll;
import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;

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
