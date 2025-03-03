/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.refcolnames.embeddedid;

import jakarta.persistence.Embeddable;

@Embeddable
class TownCode extends PostalCode {
	String town;
}
