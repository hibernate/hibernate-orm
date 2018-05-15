/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.metamodel.attributeInSuper;

import java.io.Serializable;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.EmbeddedId;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

@javax.persistence.Entity
public class WorkOrder implements Serializable {
	@EmbeddedId
	private WorkOrderId id;
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "workOrder")
	@OrderBy("operation, bomItemNumber")
	private Set<WorkOrderComponent> components;
  /* other stuffs */
}