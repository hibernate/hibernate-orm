/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.inheritance.mixed.entities;

import java.io.Serializable;

public interface Activity extends Serializable {
	ActivityId getId();

	Integer getSequenceNumber();
}
