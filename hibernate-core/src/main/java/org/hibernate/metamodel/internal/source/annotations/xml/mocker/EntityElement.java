package org.hibernate.metamodel.internal.source.annotations.xml.mocker;

import org.hibernate.jaxb.spi.orm.JaxbAccessType;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public interface EntityElement {
	String getClazz();

	void setClazz(String className);

	Boolean isMetadataComplete();

	void setMetadataComplete(Boolean isMetadataComplete);
	public JaxbAccessType getAccess();
}
