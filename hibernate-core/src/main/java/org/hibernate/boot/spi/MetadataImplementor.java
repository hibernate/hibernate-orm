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
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.query.named.NamedQueryRepository;
import org.hibernate.type.Type;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * The SPI-level Metadata contract.
 *
 * @author Steve Ebersole
 *
 * @since 5.0
 */
public interface MetadataImplementor extends Metadata, Mapping {
	/**
	 * Access to the options used to build this Metadata
	 */
	MetadataBuildingOptions getMetadataBuildingOptions();

	/**
	 * Access to the TypeConfiguration
	 *
	 * @return Access to the TypeConfiguration
	 */
	TypeConfiguration getTypeConfiguration();

	NamedQueryRepository buildNamedQueryRepository(SessionFactoryImplementor sessionFactory);

	void validate() throws MappingException;

	Set<MappedSuperclass> getMappedSuperclassMappingsCopy();
}
