/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.metamodel.attributeInSuper;

import java.io.Serializable;
import javax.persistence.Embeddable;
import javax.persistence.MappedSuperclass;

/**
 * @author Steve Ebersole
 */
@Embeddable
@MappedSuperclass
public class WorkOrderId implements Serializable {
	private String workOrder;
	private Long plantId;
    /* other stuffs */
}
