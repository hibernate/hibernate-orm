/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
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
package org.hibernate.test.annotations.entity;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@TypeDef(
		name = "phoneNumber",
		defaultForType = PhoneNumber.class,
		typeClass = PhoneNumberType.class
)

/**
 * @author Sharath Reddy
 *
 */
@Entity
public class ContactDetails {

	@Id
	@GeneratedValue
	private int id;
		
	private PhoneNumber localPhoneNumber;
	@Type(type="phoneNumber")
	private OverseasPhoneNumber overseasPhoneNumber;
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public PhoneNumber getLocalPhoneNumber() {
		return localPhoneNumber;
	}
	public void setLocalPhoneNumber(PhoneNumber localPhoneNumber) {
		this.localPhoneNumber = localPhoneNumber;
	}
	public OverseasPhoneNumber getOverseasPhoneNumber() {
		return overseasPhoneNumber;
	}
	public void setOverseasPhoneNumber(OverseasPhoneNumber overseasPhoneNumber) {
		this.overseasPhoneNumber = overseasPhoneNumber;
	}
	
}
