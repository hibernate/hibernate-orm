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
	public QualifiedSequenceName getSequenceName();

	/**
	 * Retrieve the extracted increment-size defined for the sequence.
	 *
	 * @return The extracted increment size; use a negative number to indicate the increment could not be extracted.
	 */
	public int getIncrementSize();
}
