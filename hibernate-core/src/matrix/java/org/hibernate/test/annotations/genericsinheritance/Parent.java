package org.hibernate.test.annotations.genericsinheritance;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MapKey;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;

@MappedSuperclass
public abstract class Parent<C extends Child> {

	@Id @GeneratedValue Long id;
	@MapKey @OneToMany(mappedBy="parent") Map<Long,C> children = new HashMap<Long,C>();

	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	
	
	public Map<Long,C> getChildren() {
		return children;
	}
	public void setChildren(Map<Long,C> children) {
		this.children = children;
	}
	
	
}
