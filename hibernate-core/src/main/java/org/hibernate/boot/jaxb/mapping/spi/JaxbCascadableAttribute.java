/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.spi;

/**
 * @author Steve Ebersole
 */
public interface JaxbCascadableAttribute extends JaxbPersistentAttribute {
	JaxbCascadeTypeImpl getCascade();
	void setCascade(JaxbCascadeTypeImpl value);
}
