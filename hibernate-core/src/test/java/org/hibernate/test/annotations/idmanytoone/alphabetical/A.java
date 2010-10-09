//$
package org.hibernate.test.annotations.idmanytoone.alphabetical;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

@Entity
public class A {

	@Id
	private int id;

	@OneToMany( mappedBy = "parent" )
	List<C> children;

	public A() {
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public List<C> getChildren() {
		return children;
	}

	public void setChildren(List<C> children) {
		this.children = children;
	}


}

