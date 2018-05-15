/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.domain;

import java.util.List;

import org.hibernate.boot.MappingException;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.boot.model.source.spi.LocalMetadataBuildingContext;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.type.spi.BasicType;

/**
 * A ValueMapping extension for basic-valued mappings
 *
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
public interface BasicValueMapping<J> extends ValueMapping<J> {
	/**
	 * Get the single column associated with this basic mapping
	 */
	default MappedColumn getMappedColumn() {
		final List<MappedColumn> mappedColumns = getMappedColumns();
		if ( mappedColumns.isEmpty() ) {
			return null;
		}
		else if ( mappedColumns.size() == 1 ) {
			return mappedColumns.get( 0 );
		}

		final Origin origin = getMetadataBuildingContext() instanceof LocalMetadataBuildingContext
				? ( (LocalMetadataBuildingContext) getMetadataBuildingContext() ).getOrigin()
				: null;

		throw new MappingException(
				"Basic valued mappping cannot define more than 1 column",
				origin
		);
	}

	BasicType<J> resolveType();

	ConverterDescriptor getAttributeConverterDescriptor();

	BasicValueConverter resolveValueConverter(
			RuntimeModelCreationContext creationContext,
			BasicType basicType);
}
