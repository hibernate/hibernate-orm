/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.entity;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;


/**
 * @author Sharath Reddy
 *
 */
@Entity
public class LocalContactDetails {

	@Id
	@GeneratedValue
	private int id;

	private PhoneNumber localPhoneNumber;
	@Convert( converter = PhoneNumberConverter.class )
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
