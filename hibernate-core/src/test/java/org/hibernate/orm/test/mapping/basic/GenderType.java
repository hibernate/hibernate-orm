/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import java.sql.Types;

import org.hibernate.usertype.UserTypeSupport;

/**
 * @author Vlad Mihalcea
 */
//tag::basic-enums-custom-type-example[]
public class GenderType extends UserTypeSupport<Gender> {
	public GenderType() {
		super(Gender.class, Types.CHAR);
	}
}
//end::basic-enums-custom-type-example[]
