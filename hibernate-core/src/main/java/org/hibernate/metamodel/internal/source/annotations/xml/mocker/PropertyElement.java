package org.hibernate.metamodel.internal.source.annotations.xml.mocker;

import org.hibernate.internal.jaxb.mapping.orm.JaxbAccessType;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public interface PropertyElement {
	String getName();

	JaxbAccessType getAccess();

	void setAccess(JaxbAccessType accessType);
}
