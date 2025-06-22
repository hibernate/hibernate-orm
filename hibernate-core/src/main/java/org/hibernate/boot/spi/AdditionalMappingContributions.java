/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.spi;

import java.io.InputStream;

import org.hibernate.Incubating;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.ClassDetails;

/**
 * Collector for contributions from {@linkplain AdditionalMappingContributor contributors}
 *
 * @author Steve Ebersole
 *
 * @since 6.2
 */
@Incubating
public interface AdditionalMappingContributions {
	/**
	 * Contribute a presumably annotated entity class.
	 */
	void contributeEntity(Class<?> entityType);

	/**
	 * Contribute a ClassDetails representing a "managed class" (entity, embeddable, converter, etc)
	 */
	void contributeManagedClass(ClassDetails classDetails);

	/**
	 * Contribute mappings from the InputStream containing an XML mapping document.
	 */
	void contributeBinding(InputStream xmlStream);

	/**
	 * Contribute mappings in the form of {@code hbm.xml} JAXB bindings.
	 *
	 * @deprecated {@code hbm.xml} mapping file support is deprecated.  Use
	 * {@linkplain #contributeBinding(org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl) extended orm.xml}
	 * bindings instead.
	 */
	@Deprecated
	void contributeBinding(JaxbHbmHibernateMapping hbmJaxbBinding);

	/**
	 * Contribute mappings in the form of (extended) {@code orm.xml} JAXB bindings
	 */
	void contributeBinding(JaxbEntityMappingsImpl mappingJaxbBinding);

	/**
	 * Contribute a materialized Table
	 */
	void contributeTable(Table table);

	/**
	 * Contribute a materialized Sequence
	 */
	void contributeSequence(Sequence sequence);

	/**
	 * Contribute a materialized AuxiliaryDatabaseObject
	 */
	void contributeAuxiliaryDatabaseObject(AuxiliaryDatabaseObject auxiliaryDatabaseObject);

	EffectiveMappingDefaults getEffectiveMappingDefaults();
}
