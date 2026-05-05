/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.generator.values.internal;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.metamodel.mapping.ModelPart;

import static java.util.Collections.emptyList;
import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;

/**
 * Standard implementation for {@link GeneratedValues} using {@link IdentityHashMap}.
 *
 * @author Marco Belladelli
 */
public class GeneratedValuesImpl implements GeneratedValues {
	private final Map<ModelPart, Object> generatedValuesMap;

	public GeneratedValuesImpl(List<? extends ModelPart> generatedProperties) {
		this( generatedProperties.size() );
	}

	public GeneratedValuesImpl(int generatedPropertiesCount) {
		this.generatedValuesMap = new IdentityHashMap<>( generatedPropertiesCount );
	}

	@Override
	public void addGeneratedValue(ModelPart modelPart, Object value) {
		generatedValuesMap.put( modelPart, value );
	}

	@Override
	public Object getGeneratedValue(ModelPart modelPart) {
		return generatedValuesMap.get( modelPart );
	}

	@Override
	public List<Object> getGeneratedValues(List<? extends ModelPart> modelParts) {
		if ( isEmpty( modelParts ) ) {
			return emptyList();
		}

		final List<Object> generatedValues = new ArrayList<>( modelParts.size() );
		for ( var modelPart : modelParts ) {
			assert generatedValuesMap.containsKey( modelPart );
			generatedValues.add( generatedValuesMap.get( modelPart ) );
		}

		return generatedValues;
	}

	@Override
	public void apply(GeneratedValues generatedValues) {
		if ( generatedValues == null ) {
			// this happens per-table,
			// presumably this would mean there were none for this table.
		}
		else if ( generatedValues instanceof GeneratedValuesImpl impl ) {
			generatedValuesMap.putAll( impl.generatedValuesMap );
		}
		else {
			throw new UnsupportedOperationException( "Only GeneratedValuesImpl supported : " + generatedValues.getClass().getName() );
		}
	}
}
