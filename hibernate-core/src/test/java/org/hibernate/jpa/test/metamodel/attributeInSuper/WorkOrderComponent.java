/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.metamodel.attributeInSuper;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;

/**
 * @author Steve Ebersole
 */
@jakarta.persistence.Entity
public class WorkOrderComponent {
	@EmbeddedId
	private WorkOrderComponentId id;
	@ManyToOne
	@JoinColumns({
			@JoinColumn(name = "WORK_ORDER", nullable = false, insertable = false, updatable = false),
			@JoinColumn(name = "PLANT_ID", nullable = false, insertable = false, updatable = false)
	})
	private WorkOrder workOrder;
}
