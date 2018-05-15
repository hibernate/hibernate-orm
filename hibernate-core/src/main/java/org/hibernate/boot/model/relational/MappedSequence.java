/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.relational;

import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.metamodel.model.relational.spi.PhysicalNamingStrategy;
import org.hibernate.metamodel.model.relational.spi.Sequence;
import org.hibernate.naming.QualifiedSequenceName;

/**
 * @author Steve Ebersole
 */
public interface MappedSequence extends Loggable {
	/**
	 * Get the qualified name for this MappedSequence, including namespace
	 * name (catalog, schema).  The actual "physical name" (see
	 * {@link org.hibernate.boot.model.naming.PhysicalNamingStrategy#toPhysicalSequenceName}) is not
	 * determined until later
	 */
	QualifiedSequenceName getLogicalName();

	int getInitialValue();

	int getIncrementSize();

	void validate(int initialValue, int incrementSize);

	Sequence generateRuntimeSequence(PhysicalNamingStrategy namingStrategy, JdbcEnvironment jdbcEnvironment);
}
