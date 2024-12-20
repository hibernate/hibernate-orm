/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metamodel.attributeInSuper;

import jakarta.persistence.Embeddable;

/**
 * @author Steve Ebersole
 */
@Embeddable
public class WorkOrderComponentId extends AbstractWorkOrderId {
	private Long lineNumber;
	/* other stuffs */
}
