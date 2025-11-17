/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.spi;

import org.hibernate.boot.jaxb.mapping.spi.JaxbFilterImpl;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;

/**
 * Commonality for filter annotations
 *
 * @see org.hibernate.annotations.Filter
 * @see org.hibernate.annotations.FilterJoinTable
 *
 * @author Steve Ebersole
 */
public interface FilterDetails {
	void apply(JaxbFilterImpl jaxbFilter, XmlDocumentContext xmlDocumentContext);
}
