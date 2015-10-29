/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.metamodel.attributeInSuper;

import java.util.Set;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(WorkOrder.class)
public class WorkOrder_ {
	public static volatile SingularAttribute<WorkOrder, WorkOrderId> id;
	public static volatile PluralAttribute<WorkOrder, Set, WorkOrderComponent> components;

}
