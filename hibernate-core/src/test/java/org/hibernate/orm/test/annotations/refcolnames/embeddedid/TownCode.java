/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.refcolnames.embeddedid;

import jakarta.persistence.Embeddable;

@Embeddable
class TownCode extends PostalCode {
	String town;
}
