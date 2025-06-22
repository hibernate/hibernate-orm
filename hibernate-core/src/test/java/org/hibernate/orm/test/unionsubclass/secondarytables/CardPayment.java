/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.unionsubclass.secondarytables;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;

import java.time.ZonedDateTime;

/**
 * @author Steve Ebersole
 */
@Entity
@SecondaryTable(
		name = "authorizations",
		pkJoinColumns = @PrimaryKeyJoinColumn(name = "charge_auth_fk", referencedColumnName = "id")
)
public class CardPayment extends Payment {
	ZonedDateTime paymentMade;

	@Column(table = "authorizations")
	String authorizationCode;
}
