/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009, Red Hat Inc. or third-party contributors as
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
package org.hibernate.ejb.metamodel;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
@Entity
@Table(name = "PHONE_TABLE")
public class Phone implements java.io.Serializable {
	private String id;
	private String area;
	private String number;
	private Address address;

	public Phone() {
	}

	public Phone(String v1, String v2, String v3) {
		id = v1;
		area = v2;
		number = v3;
	}

	public Phone(String v1, String v2, String v3, Address v4) {
		id = v1;
		area = v2;
		number = v3;
		address = v4;
	}

	@Id
	@Column(name = "ID")
	public String getId() {
		return id;
	}

	public void setId(String v) {
		id = v;
	}

	@Column(name = "AREA")
	public String getArea() {
		return area;
	}

	public void setArea(String v) {
		area = v;
	}

	@Column(name = "PHONE_NUMBER")
	public String getNumber() {
		return number;
	}

	public void setNumber(String v) {
		number = v;
	}

	@ManyToOne
	@JoinColumn(name = "FK_FOR_ADDRESS")
	public Address getAddress() {
		return address;
	}

	public void setAddress(Address a) {
		address = a;
	}
}