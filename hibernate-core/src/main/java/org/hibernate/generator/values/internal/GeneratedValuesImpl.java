/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.generator.values.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.ModelPart;

/**
 * Standard implementation for {@link GeneratedValues} using {@link IdentityHashMap}.
 *
 * @author Marco Belladelli
 */
public class GeneratedValuesImpl implements GeneratedValues {
	private final Map<ModelPart, Object> generatedValuesMap;

	public GeneratedValuesImpl(List<? extends ModelPart> generatedProperties) {
		this.generatedValuesMap = new IdentityHashMap<>( generatedProperties.size() );
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
		if ( CollectionHelper.isEmpty( modelParts ) ) {
			return Collections.emptyList();
		}

		final List<Object> generatedValues = new ArrayList<>( modelParts.size() );
		for ( ModelPart modelPart : modelParts ) {
			assert generatedValuesMap.containsKey( modelPart );
			generatedValues.add( generatedValuesMap.get( modelPart ) );
		}

		return generatedValues;
	}
}
