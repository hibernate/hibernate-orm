//$Id$
package org.hibernate.jpa.test.emops.cascade;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
public class C1 {

	@Id
	@GeneratedValue( strategy = GenerationType.AUTO )
	private int id;

	@ManyToOne( fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST} )
	@JoinColumn( name = "b1Id" )
	private B1 b1;

	public B1 getB1() {
		return b1;
	}

	public void setB1(B1 b1) {
		this.b1 = b1;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
}
