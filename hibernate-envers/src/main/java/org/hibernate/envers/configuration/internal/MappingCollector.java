/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.configuration.internal;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;

/**
 * Used in building the AuditConfiguration to allow callbacks for generated audit entities.
 *
 * The idea here is to allow a Envers to "callback" with any
 *
 * @author Steve Ebersole
 * @author Chris Cranford
 */
public interface MappingCollector {
	void addDocument(JaxbHbmHibernateMapping mapping);
}
