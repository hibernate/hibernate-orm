/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.metamodel.attributeInSuper;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * @author Steve Ebersole
 */
@StaticMetamodel(WorkOrderComponent.class)
public class WorkOrderComponent_ {
	public static volatile SingularAttribute<WorkOrderComponent, WorkOrderComponentId> id;
	public static volatile SingularAttribute<WorkOrderComponent, WorkOrder> workOrder;

}
