/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.relational.spi;

import org.hibernate.boot.model.relational.Exportable;
import org.hibernate.naming.QualifiedSequenceName;
import org.hibernate.naming.Identifier;

/**
 * @author Steve Ebersole
 */
public interface Sequence extends Exportable {
	String getLoggableView();

	int getInitialValue();

	int getIncrementSize();

	Identifier getName();

	QualifiedSequenceName getQaulifiedName();
}
