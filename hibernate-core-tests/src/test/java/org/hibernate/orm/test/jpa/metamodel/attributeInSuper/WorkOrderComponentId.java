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
