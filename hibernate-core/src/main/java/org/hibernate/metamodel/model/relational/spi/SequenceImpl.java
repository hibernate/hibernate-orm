/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.model.relational.spi;

import org.hibernate.naming.Identifier;
import org.hibernate.naming.QualifiedSequenceName;

/**
 * @author Andrea Boriero
 */
public class SequenceImpl implements Sequence {
	@Override
	public int getInitialValue() {
		return 0;
	}

	@Override
	public int getIncrementSize() {
		return 0;
	}

	@Override
	public Identifier getName() {
		return null;
	}

	@Override
	public QualifiedSequenceName getQualifiedName() {
		return null;
	}

	@Override
	public String getExportIdentifier() {
		return getName().render();
	}

	@Override
	public String toString() {
		return "Sequence(" + getName() + ")";
	}

}
