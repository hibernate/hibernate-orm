/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metamodel.attributeInSuper;

import java.io.Serializable;

import jakarta.persistence.MappedSuperclass;

/**
 * @author Steve Ebersole
 */
@MappedSuperclass
public class AbstractWorkOrderId implements Serializable {
	private String workOrder;
	private Long plantId;
	/* other stuffs */
}
