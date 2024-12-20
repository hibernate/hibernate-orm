/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.entity;


/**
 * @author Sharath Reddy
 */
public class OverseasPhoneNumber extends PhoneNumber {

	public OverseasPhoneNumber(String areaCode, String val) {
		super(areaCode + val);
	}

	public OverseasPhoneNumber(String val) {
		super(val);
	}

}
