/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.ast.internal;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.NaturalIdPostLoadListener;
import org.hibernate.loader.NaturalIdPreLoadListener;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.internal.CompoundNaturalIdMapping;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;

/**
 * NaturalIdLoader implementation for compound natural-ids
 */
public class CompoundNaturalIdLoader<T> extends AbstractNaturalIdLoader<T> {

	public CompoundNaturalIdLoader(
			CompoundNaturalIdMapping naturalIdMapping,
			NaturalIdPreLoadListener preLoadListener,
			NaturalIdPostLoadListener postLoadListener,
			EntityMappingType entityDescriptor,
			MappingModelCreationProcess creationProcess) {
		super( naturalIdMapping, preLoadListener, postLoadListener, entityDescriptor, creationProcess );
	}

	@Override
	protected Object resolveNaturalIdBindValue(Object naturalIdValue, SharedSessionContractImplementor session) {
		// the "real" form as an array, although we also accept here a Map and reduce it to
		// the appropriately ordered array
		if ( naturalIdValue instanceof Object[] ) {
			return naturalIdValue;
		}

		final List<SingularAttributeMapping> attributes = naturalIdMapping().getNaturalIdAttributes();
		final Object[] naturalId = new Object[ attributes.size() ];

		if ( naturalIdValue instanceof Map ) {
			final Map<String,?> valueMap = (Map<String,?>) naturalIdValue;
			for ( int i = 0; i < attributes.size(); i++ ) {
				final SingularAttributeMapping attributeMapping = attributes.get( i );
				naturalId[ i ] = valueMap.get( attributeMapping.getAttributeName() );
			}
			return naturalId;
		}

		throw new IllegalArgumentException( "Unexpected natural-id reference [" + naturalIdValue + "; expecting array or Map" );
	}

	@Override
	protected void applyNaturalIdAsJdbcParameters(
			Object naturalIdToLoad,
			List<JdbcParameter> jdbcParameters,
			JdbcParameterBindings jdbcParamBindings,
			SharedSessionContractImplementor session) {
		assert naturalIdToLoad instanceof Object[];
		final Object[] naturalIdValueArray = (Object[]) naturalIdToLoad;

		final Iterator<JdbcParameter> jdbcParamItr = jdbcParameters.iterator();

		for ( int i = 0; i < naturalIdMapping().getNaturalIdAttributes().size(); i++ ) {
			final SingularAttributeMapping attrMapping = naturalIdMapping().getNaturalIdAttributes().get( i );
			attrMapping.visitJdbcValues(
					naturalIdValueArray[i],
					Clause.WHERE,
					(jdbcValue, jdbcMapping) -> {
						assert jdbcParamItr.hasNext();
						final JdbcParameter jdbcParam = jdbcParamItr.next();
						jdbcParamBindings.addBinding(
								jdbcParam,
								new JdbcParameterBindingImpl( jdbcMapping, jdbcValue )
						);
					},
					session
			);
		}

		// make sure we've exhausted all JDBC parameters
		assert ! jdbcParamItr.hasNext();
	}

	@Override
	public Object resolveIdToNaturalId(Object id, SharedSessionContractImplementor session) {
		return null;
	}

	@Override
	protected boolean isSimple() {
		return false;
	}
}
