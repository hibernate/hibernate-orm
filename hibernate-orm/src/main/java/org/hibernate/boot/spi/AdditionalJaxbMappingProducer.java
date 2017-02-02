/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.spi;

import java.util.Collection;

import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.model.source.internal.hbm.MappingDocument;

import org.jboss.jandex.IndexView;

/**
 * @author Steve Ebersole
 *
 * @deprecated Intended for Envers integration until we can migrate Envers away from XML generation
 * for building its model.
 */
@Deprecated
public interface AdditionalJaxbMappingProducer {
	Collection<MappingDocument> produceAdditionalMappings(
			MetadataImplementor metadata,
			IndexView jandexIndex,
			MappingBinder mappingBinder,
			MetadataBuildingContext buildingContext);
}
