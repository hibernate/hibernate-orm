/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metamodel.attributeInSuper;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * @author Steve Ebersole
 */
@jakarta.persistence.Entity
public class WorkOrderComponent {
	@EmbeddedId
	private WorkOrderComponentId id;
	@ManyToOne
	@JoinColumn(name = "WORK_ORDER", nullable = false, insertable = false, updatable = false)
	@JoinColumn(name = "PLANT_ID", nullable = false, insertable = false, updatable = false)
	private WorkOrder workOrder;
}
