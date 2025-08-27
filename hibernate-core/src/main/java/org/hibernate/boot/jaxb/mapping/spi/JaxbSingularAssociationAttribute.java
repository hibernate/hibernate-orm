/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.spi;

/**
 * @author Steve Ebersole
 */
public interface JaxbSingularAssociationAttribute extends JaxbSingularAttribute, JaxbStandardAttribute, JaxbAssociationAttribute {
	JaxbSingularFetchModeImpl getFetchMode();
	void setFetchMode(JaxbSingularFetchModeImpl mode);
}
