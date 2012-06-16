//$Id$
package org.hibernate.test.annotations.xml.hbm;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

@Entity
@Inheritance( strategy = InheritanceType.JOINED )
@org.hibernate.annotations.Proxy( proxyClass = A.class )
@Table( name = "A" )
public class AImpl implements A {
	private static final long serialVersionUID = 1L;

	private Integer aId = 0;

	public AImpl() {
	}

	@Id
	@GeneratedValue
	@Column( name = "aID" )
	public Integer getAId() {
		return this.aId;
	}

	public void setAId(Integer aId) {
		this.aId = aId;
	}
}
