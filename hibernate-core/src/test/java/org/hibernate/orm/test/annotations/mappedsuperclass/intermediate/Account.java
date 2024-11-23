/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.mappedsuperclass.intermediate;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

/**
 * The intermediate entity in the hierarchy
 *
 * @author Saša Obradović
 */
@Entity
@Table(name = "`ACCOUNT`")
@Inheritance(strategy = InheritanceType.JOINED)
public class Account extends AccountBase {
	public Account() {
	}

	public Account(String accountNumber) {
		super( accountNumber );
	}
}
