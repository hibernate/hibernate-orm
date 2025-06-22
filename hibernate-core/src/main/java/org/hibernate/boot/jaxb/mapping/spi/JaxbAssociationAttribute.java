/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.spi;

/**
 * JAXB binding interface for association attributes (to-one and plural mappings)
 *
 * @author Steve Ebersole
 */
public interface JaxbAssociationAttribute extends JaxbCascadableAttribute {

	String getTargetEntity();
	void setTargetEntity(String value);
}
