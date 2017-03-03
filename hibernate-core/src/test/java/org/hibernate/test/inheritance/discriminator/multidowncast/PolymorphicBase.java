/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.inheritance.discriminator.multidowncast;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;

/**
 * @author Christian Beikov
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class PolymorphicBase implements Serializable {
	private static final long serialVersionUID = 1L;

	private Long id;
	private String name;
	private PolymorphicBase parent;
	private List<PolymorphicBase> list = new ArrayList<PolymorphicBase>();
	private Set<PolymorphicBase> children = new HashSet<PolymorphicBase>();
	private Map<String, PolymorphicBase> map = new HashMap<String, PolymorphicBase>();

	public PolymorphicBase() {
	}

	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	public PolymorphicBase getParent() {
		return parent;
	}

	public void setParent(PolymorphicBase parent) {
		this.parent = parent;
	}

	@OneToMany
	@OrderColumn(name = "list_idx", nullable = false)
	@JoinTable(name = "polymorphic_list")
	public List<PolymorphicBase> getList() {
		return list;
	}

	public void setList(List<PolymorphicBase> list) {
		this.list = list;
	}

	@OneToMany(mappedBy = "parent")
	public Set<PolymorphicBase> getChildren() {
		return children;
	}

	public void setChildren(Set<PolymorphicBase> children) {
		this.children = children;
	}

	@OneToMany
	@JoinTable(name = "polymorphic_map")
	@MapKeyColumn(length = 20, nullable = false)
	public Map<String, PolymorphicBase> getMap() {
		return map;
	}

	public void setMap(Map<String, PolymorphicBase> map) {
		this.map = map;
	}
}
