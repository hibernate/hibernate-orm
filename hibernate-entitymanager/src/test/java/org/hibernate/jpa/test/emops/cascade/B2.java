//$Id$
package org.hibernate.jpa.test.emops.cascade;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

@Entity
public class B2 {

	@Id
	@GeneratedValue( strategy = GenerationType.AUTO )
	private int id;

	@ManyToOne( fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST} )
	@JoinColumn( name = "aId" )
	private A a;

	@OneToMany( fetch = FetchType.LAZY, mappedBy = "b2", cascade = {CascadeType.PERSIST} )
	private Set<C2> c2List;

	public A getA() {
		return a;
	}

	public void setA(A a) {
		this.a = a;
	}

	public Set<C2> getC2List() {
		if ( c2List == null )
			c2List = new HashSet<C2>();
		return c2List;
	}

	public void setC2List(Set<C2> list) {
		c2List = list;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
}
