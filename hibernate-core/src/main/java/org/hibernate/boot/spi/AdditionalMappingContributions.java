/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.spi;

import org.hibernate.Incubating;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;

/**
 * Collector for contributions from {@linkplain AdditionalMappingContributor contributors}
 *
 * @author Steve Ebersole
 */
@Incubating
public interface AdditionalMappingContributions {
	void contributeBinding(JaxbHbmHibernateMapping hbmJaxbBinding, Origin origin);
	void contributeEntity(PersistentClass entity);
	void contributeTable(Table table);
	void contributeSequence(Sequence sequence);
	void contributeAuxiliaryDatabaseObject(AuxiliaryDatabaseObject auxiliaryDatabaseObject);
}
