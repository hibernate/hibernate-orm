/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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


