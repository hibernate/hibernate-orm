/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.spi;

import java.util.Set;

import org.hibernate.MappingException;
import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.internal.NamedQueryRepository;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.type.TypeResolver;

/**
 * The SPI-level Metadata contract.
 *
 * @todo Should Mapping be implemented here, or on InFlightMetadataCollector instead?
 *
 * @author Steve Ebersole
 *
 * @since 5.0
 */
public interface MetadataImplementor extends Metadata, Mapping {
	/**
	 * Access to the options used to build this Metadata
	 *
	 * @return
	 */
	MetadataBuildingOptions getMetadataBuildingOptions();

	TypeResolver getTypeResolver();

	NamedQueryRepository buildNamedQueryRepository(SessionFactoryImpl sessionFactory);

	void validate() throws MappingException;

	Set<MappedSuperclass> getMappedSuperclassMappingsCopy();
}
