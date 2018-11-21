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
import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.boot.model.source.spi.LocalMetadataBuildingContext;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.metamodel.model.domain.spi.BasicValueMapper;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
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

		throw new MappingException( "Basic valued mapping cannot define more than 1 column", origin );
	}

	/**
	 * Get this mapping's resolution
	 *
	 * @throws NotYetResolvedException If the mapping has not yet been resolved - see {@link #resolve}
	 */
	Resolution<J> getResolution() throws NotYetResolvedException;

	/**
	 * Resolved form of {@link BasicValueMapping} as part of interpreting the
	 * boot-time model into the run-time model
	 *
	 * @author Steve Ebersole
	 */
	interface Resolution<J> {
		/**
		 * The associated BasicType
		 */
		BasicType getBasicType();

		/**
		 * The indicated mapper
		 */
		BasicValueMapper<J> getValueMapper();

		/**
		 * The JavaTypeDescriptor for the value as part of the domain model
		 */
		BasicJavaDescriptor<J> getDomainJavaDescriptor();

		/**
		 * The JavaTypeDescriptor for the relational value as part of
		 * the relational model (its JDBC representation)
		 */
		BasicJavaDescriptor<?> getRelationalJavaDescriptor();

		/**
		 * The JavaTypeDescriptor for the relational value as part of
		 * the relational model (its JDBC representation)
		 */
		SqlTypeDescriptor getRelationalSqlTypeDescriptor();

		/**
		 * Converter, if any, to convert values between the
		 * domain and relational JavaTypeDescriptor representations
		 */
		BasicValueConverter getValueConverter();

		/**
		 * The resolved MutabilityPlan
		 */
		MutabilityPlan<J> getMutabilityPlan();
	}
}
