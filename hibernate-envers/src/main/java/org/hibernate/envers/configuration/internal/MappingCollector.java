/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
