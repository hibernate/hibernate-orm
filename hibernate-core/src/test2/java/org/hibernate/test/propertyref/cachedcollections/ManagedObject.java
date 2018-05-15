/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.propertyref.cachedcollections;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings({"UnusedDeclaration"})
public class ManagedObject {
	private Long moid;
	private int version;
	private String name = "parent";
	private String displayName = "";
	private Set<String> members = new HashSet<String>();

	public ManagedObject() {
	}

	public ManagedObject(String name, String displayName) {
		this.name = name;
		this.displayName = displayName;
	}

	public Long getMoid() {
		return this.moid;
	}

	private void setMoid(Long moid) {
		this.moid = moid;
	}

	public int getVersion() {
		return this.version;
	}

	private void setVersion(int version) {
		this.version = version;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String dName) {
		this.displayName = dName;
	}

	public Set<String> getMembers() {
		return this.members;
	}

	public void setMembers(Set<String> members1) {
		this.members = members1;
	}

}


