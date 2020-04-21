/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.metamodel.attributeInSuper;

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