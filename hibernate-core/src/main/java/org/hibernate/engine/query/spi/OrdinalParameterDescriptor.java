/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.query.spi;

import org.hibernate.Incubating;
import org.hibernate.type.Type;

/**
 * Descriptor regarding an ordinal parameter.
 *
 * @author Steve Ebersole
 */
@Incubating
public class OrdinalParameterDescriptor extends AbstractParameterDescriptor {
	private final int label;
	private final int valuePosition;

	/**
	 * Constructs an ordinal parameter descriptor.
	 */
	public OrdinalParameterDescriptor(
			int label,
			int valuePosition,
			Type expectedType,
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
