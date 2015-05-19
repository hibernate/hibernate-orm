/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import java.io.Serializable;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * Basic implementation of the {@link PostInsertIdentifierGenerator} contract.
 *
 * @author Gavin King
 */
public abstract class AbstractPostInsertGenerator
		implements PostInsertIdentifierGenerator, BulkInsertionCapableIdentifierGenerator {
	@Override
	public Serializable generate(SessionImplementor s, Object obj) {
		return IdentifierGeneratorHelper.POST_INSERT_INDICATOR;
	}

	@Override
	public boolean supportsBulkInsertionIdentifierGeneration() {
		return true;
	}

	@Override
	public String determineBulkInsertionIdentifierGenerationSelectFragment(Dialect dialect) {
		return null;
	}
}
