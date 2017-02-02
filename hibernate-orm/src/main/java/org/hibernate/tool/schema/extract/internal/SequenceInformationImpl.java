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
	private final int incrementSize;

	public SequenceInformationImpl(QualifiedSequenceName sequenceName, int incrementSize) {
		this.sequenceName = sequenceName;
		this.incrementSize = incrementSize;
	}

	@Override
	public QualifiedSequenceName getSequenceName() {
		return sequenceName;
	}

	@Override
	public int getIncrementSize() {
		return incrementSize;
	}
}
