/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.inheritance.mixed.entities;

import java.io.Serializable;

public interface Activity extends Serializable {
	ActivityId getId();

	Integer getSequenceNumber();
}
