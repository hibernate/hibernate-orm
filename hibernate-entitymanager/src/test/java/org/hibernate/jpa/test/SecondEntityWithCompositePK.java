/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.test;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "VYA6CPP")
public class SecondEntityWithCompositePK implements Serializable {

	@EmbeddedId
	private SecondCompositePK id;

	@Column(name = "SOMESTRINGVALUE")
	private String someStringValue;

	// bi-directional one-to-one association to FirstEntityWithCompositePK
	@OneToOne(mappedBy = "secondEntityWithCompositePK", fetch = FetchType.EAGER)
	private FirstEntityWithCompositePK firstEntityWithCompositePK;

	public SecondCompositePK getId() {
		return id;
	}

	public void setId(SecondCompositePK id) {
		this.id = id;
	}

	public String getSomeStringValue() {
		return someStringValue;
	}

	public void setSomeStringValue(String someStringValue) {
		this.someStringValue = someStringValue;
	}

	public FirstEntityWithCompositePK getFirstEntityWithCompositePK() {
		return firstEntityWithCompositePK;
	}

	public void setFirstEntityWithCompositePK(FirstEntityWithCompositePK firstEntityWithCompositePK) {
		this.firstEntityWithCompositePK = firstEntityWithCompositePK;
	}
}