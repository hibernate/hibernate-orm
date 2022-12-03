/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.generator.internal;

import org.hibernate.annotations.GeneratedColumn;
import org.hibernate.dialect.Dialect;
import org.hibernate.generator.InDatabaseGenerator;

/**
 * For {@link GeneratedColumn}.
 *
 * @author Gavin King
 */
public class GeneratedAlwaysGeneration implements InDatabaseGenerator {

	public GeneratedAlwaysGeneration() {}

	@Override
	public boolean generatedOnUpdate() {
		return true;
	}

	@Override
	public boolean generatedOnInsert() {
		return true;
	}

	@Override
	public boolean writePropertyValue() {
		return false;
	}

	@Override
	public boolean referenceColumnsInSql(Dialect dialect) {
		return false;
	}

	@Override
	public String[] getReferencedColumnValues(Dialect dialect) {
		return null;
	}
}
