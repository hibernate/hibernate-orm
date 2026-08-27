/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.integrationtest.java.module.test.annotation;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity(name = "ConverterSubjectEntity")
public class ConverterSubjectEntity {
	@Id
	public Long id;

	public StubConvertibleType convertible;
}
