/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.spi;

import jakarta.persistence.FetchType;

/**
 * Commonality between non-id, non-version and non-embedded.  Basically attributes that JPA
 * defines as fetchable or not.
 *
 * @author Steve Ebersole
 */
public interface JaxbStandardAttribute extends JaxbPersistentAttribute {
	FetchType getFetch();
	void setFetch(FetchType value);

	Boolean isOptional();
	void setOptional(Boolean optional);
}
