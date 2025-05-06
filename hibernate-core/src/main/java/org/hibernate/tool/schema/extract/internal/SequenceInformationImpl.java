/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.extract.internal;

import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;

/**
 * For now we only collect sequence name.  If all databases support it, would really like to see INCREMENT here as well.
 *
 * @author Steve Ebersole
 */
public class SequenceInformationImpl implements SequenceInformation {
	private final QualifiedSequenceName sequenceName;

	private final Number startValue;
	private final Number minValue;
	private final Number maxValue;
	private final Number incrementValue;

	public SequenceInformationImpl(
			QualifiedSequenceName sequenceName,
			Number startValue,
			Number minValue,
			Number maxValue, Number incrementValue) {
		this.sequenceName = sequenceName;
		this.startValue = startValue;
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.incrementValue = incrementValue;
	}

	@Override
	public QualifiedSequenceName getSequenceName() {
		return sequenceName;
	}

	@Override
	public Number getStartValue() {
		return startValue;
	}

	@Override
	public Number getMinValue() {
		return minValue;
	}

	public Number getMaxValue() {
		return maxValue;
	}

	@Override
	public Number getIncrementValue() {
		return incrementValue;
	}
}
