/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

	private final Long startValue;
	private final Long minValue;
	private final Long maxValue;
	private final Long incrementValue;

	public SequenceInformationImpl(
			QualifiedSequenceName sequenceName,
			Long startValue,
			Long minValue,
			Long maxValue, Long incrementValue) {
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
	public Long getStartValue() {
		return startValue;
	}

	@Override
	public Long getMinValue() {
		return minValue;
	}

	public Long getMaxValue() {
		return maxValue;
	}

	@Override
	public Long getIncrementValue() {
		return incrementValue;
	}
}
