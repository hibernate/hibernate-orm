/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.xml.hbm;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Inheritance( strategy = InheritanceType.JOINED )
@Table( name = "ENTITYZ" )
public class ZImpl implements Z {
	private static final long serialVersionUID = 1L;

	private Integer zId = null;
	private B b = null;

	@Id
	@GeneratedValue
	@Column( name = "zID" )
	public Integer getZId() {
		return zId;
	}

	public void setZId(Integer zId) {
		this.zId = zId;
	}

	@ManyToOne( optional = false, targetEntity = BImpl.class, fetch = FetchType.LAZY )
	@JoinColumn( name = "bID", referencedColumnName = "bID")
	public B getB() {
		return b;
	}

	public void setB(B b) {
		this.b = b;
	}
}
