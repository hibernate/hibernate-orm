package org.hibernate.test.annotations.genericsinheritance;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class Child<P extends Parent> {

	@Id Long id;
	@ManyToOne P parent;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public P getParent() {
		return parent;
	}
	public void setParent(P parent) {
		this.parent = parent;
	}
	
	
}
