package org.hibernate.test.bytecode.enhancement.cascade;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

/**
 * Created by barreiro on 12/9/15.
 */
@Entity
public class Parent {
	private Long id;
	private String name;
	private List<Child> children = new ArrayList<Child>();
	private String lazy;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@OneToMany(mappedBy = "parent", cascade = {
			CascadeType.PERSIST, CascadeType.MERGE,
			CascadeType.REFRESH, CascadeType.REMOVE
	},
			fetch = FetchType.LAZY)
	public List<Child> getChildren() {
		return children;
	}

	public void setChildren(List<Child> children) {
		this.children = children;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Basic(fetch = FetchType.LAZY)
	public String getLazy() {
		return lazy;
	}

	public void setLazy(String lazy) {
		this.lazy = lazy;
	}

	Child makeChild() {
		final Child c = new Child();
		c.setParent( this );
		this.children.add( c );
		return c;
	}
}
