/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import org.hibernate.dialect.Dialect;
import org.hibernate.id.factory.spi.StandardGenerator;
import org.hibernate.tuple.InDatabaseGenerator;

/**
 * An {@link InDatabaseGenerator} that handles {@code IDENTITY}/"autoincrement" columns
 * on those databases which support them.
 * <p>
 * Delegates to the {@link org.hibernate.dialect.identity.IdentityColumnSupport} provided
 * by the {@linkplain Dialect#getIdentityColumnSupport() dialect}.
 *
 * @author Christoph Sturm
 */
public class IdentityGenerator
		implements PostInsertIdentifierGenerator, BulkInsertionCapableIdentifierGenerator, StandardGenerator {
	@Override
	public boolean referenceColumnsInSql(Dialect dialect) {
		return dialect.getIdentityColumnSupport().hasIdentityInsertKeyword();
	}

	@Override
	public String[] getReferencedColumnValues(Dialect dialect) {
		return new String[] { dialect.getIdentityColumnSupport().getIdentityInsertString() };
	}

}
