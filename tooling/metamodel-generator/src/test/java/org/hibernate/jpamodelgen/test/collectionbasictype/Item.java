/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.collectionbasictype;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Chris Cranford
 */
@Entity
public class Item {
	@Id
	@GeneratedValue
	private Integer id;

	@ElementCollection
	@Convert(converter = ItemRoleConverter.class)
	private Set<Role> roles = new HashSet<>();

	@ElementCollection
	@Convert(converter = StringToUppercaseConverter.class)
	private List<String> providers = new ArrayList<>();

	@ElementCollection
	@Convert(converter = StringToUppercaseConverter.class)
	private Map<Integer, String> attributes = new HashMap<>();

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Set<Role> getRoles() {
		return roles;
	}

	public void setRoles(Set<Role> roles) {
		this.roles = roles;
	}

	public List<String> getProviders() {
		return providers;
	}

	public void setProviders(List<String> providers) {
		this.providers = providers;
	}

	public Map<Integer, String> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<Integer, String> attributes) {
		this.attributes = attributes;
	}
}
