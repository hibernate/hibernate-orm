/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.query.spi;

import org.hibernate.Incubating;
import org.hibernate.type.BindableType;

/**
 * Descriptor regarding an ordinal parameter.
 *
 * @author Steve Ebersole
 */
@Incubating
public class OrdinalParameterDescriptor<T> extends AbstractParameterDescriptor<T> {
	private final int label;
	private final int valuePosition;

	/**
	 * Constructs an ordinal parameter descriptor.
	 */
	public OrdinalParameterDescriptor(
			int label,
			int valuePosition,
			BindableType<T> expectedType,
			int[] sourceLocations) {
		super( sourceLocations, expectedType );
		this.label = label;
		this.valuePosition = valuePosition;
	}

	@Override
	public Integer getPosition() {
		return label;
	}

	public int getValuePosition() {
		return valuePosition;
	}


}
