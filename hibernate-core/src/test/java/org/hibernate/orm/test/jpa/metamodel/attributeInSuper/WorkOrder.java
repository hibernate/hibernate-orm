/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metamodel.attributeInSuper;

import java.io.Serializable;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

@jakarta.persistence.Entity
public class WorkOrder implements Serializable {
	@EmbeddedId
	private WorkOrderId id;
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "workOrder")
	@OrderBy("operation, bomItemNumber")
	private Set<WorkOrderComponent> components;
/* other stuffs */
}
