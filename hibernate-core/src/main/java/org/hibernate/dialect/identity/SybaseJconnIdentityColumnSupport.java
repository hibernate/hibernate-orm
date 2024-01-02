/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.identity;

import org.hibernate.dialect.Dialect;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.id.insert.GetGeneratedKeysDelegate;
import org.hibernate.id.insert.SybaseJConnGetGeneratedKeysDelegate;
import org.hibernate.persister.entity.EntityPersister;

public class SybaseJconnIdentityColumnSupport extends AbstractTransactSQLIdentityColumnSupport {
	public static final SybaseJconnIdentityColumnSupport INSTANCE = new SybaseJconnIdentityColumnSupport();

	@Override
	public GetGeneratedKeysDelegate buildGetGeneratedKeysDelegate(EntityPersister persister) {
		return new SybaseJConnGetGeneratedKeysDelegate( persister );
	}
}
