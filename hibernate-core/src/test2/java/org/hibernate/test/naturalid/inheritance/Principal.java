/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.naturalid.inheritance;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.NaturalId;

/**
 * @author Emanuel Kupcik
 * @author Steve Ebersole
 */
@Entity
@Inheritance( strategy = InheritanceType.JOINED )
@Table( name = "GK_PRINCIPAL" )
public abstract class Principal {
	private Long id;
	private String uid;

	protected Principal() {
	}

	protected Principal(String uid) {
		this.uid = uid;
	}

	@Id
	@GeneratedValue( generator = "increment" )
	@GenericGenerator( name = "increment", strategy = "increment" )
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@NaturalId(mutable=true)
	@Column(name = "P_UID")
	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}
}
