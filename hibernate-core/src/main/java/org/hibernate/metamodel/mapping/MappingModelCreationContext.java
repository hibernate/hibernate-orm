/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.DomainMetamodel;

/**
 * A context for creation of Hibernate's mapping model.  This process
 * occurs just after the persister model has been created and "post initialized"
 *
 * @author Steve Ebersole
 */
public interface MappingModelCreationContext {
	SessionFactoryImplementor getSessionFactory();

	DomainMetamodel getDomainModel();

	MetadataImplementor getBootModel();
}
