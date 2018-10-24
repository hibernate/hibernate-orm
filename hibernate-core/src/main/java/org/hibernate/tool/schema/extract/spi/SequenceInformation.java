/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.extract.spi;

import org.hibernate.boot.model.relational.QualifiedSequenceName;

/**
 * Access to information about existing sequences.
 *
 * @author Steve Ebersole
 */
public interface SequenceInformation {

	/**
	 * The qualified sequence name.
	 *
	 * @return The sequence name
	 */
	QualifiedSequenceName getSequenceName();

	/**
	 * Retrieve the extracted increment-size defined for the sequence.
	 *
	 * @return The extracted increment size; use a negative number to indicate the increment could not be extracted.
	 *
	 * @deprecated use {@link #getIncrementValue()} instead.
	 */
	@Deprecated
	default int getIncrementSize() {
		Long incrementSize = getIncrementValue();
		return incrementSize != null ? incrementSize.intValue() : -1;
	}

	/**
	 * Retrieve the extracted start value defined for the sequence.
	 *
	 * @return The extracted start value or null id the value could not be extracted.
	 */
	Long getStartValue();

	/**
	 * Retrieve the extracted minimum value defined for the sequence.
	 *
	 * @return The extracted minimum value or null id the value could not be extracted.
	 */
	Long getMinValue();

	/**
	 * Retrieve the extracted maximum value defined for the sequence.
	 *
	 * @return The extracted maximum value or null id the value could not be extracted.
	 */
	Long getMaxValue();

	/**
	 * Retrieve the extracted increment value defined for the sequence.
	 *
	 * @return The extracted increment value; use a negative number to indicate the increment could not be extracted.
	 */
	Long getIncrementValue();
}
