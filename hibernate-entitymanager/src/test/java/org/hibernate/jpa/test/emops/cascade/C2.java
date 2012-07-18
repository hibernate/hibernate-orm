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
public class C2 {

	@Id
	@GeneratedValue( strategy = GenerationType.AUTO )
	private int id;

	@ManyToOne( fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST} )
	@JoinColumn( name = "b2Id" )
	private B2 b2;

	public B2 getB2() {
		return b2;
	}

	public void setB2(B2 b2) {
		this.b2 = b2;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
}
