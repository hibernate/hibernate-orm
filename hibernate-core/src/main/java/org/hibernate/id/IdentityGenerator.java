/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import org.hibernate.dialect.Dialect;
import org.hibernate.id.factory.spi.StandardGenerator;
import org.hibernate.generator.InDatabaseGenerator;
import org.hibernate.id.insert.BasicSelectingDelegate;
import org.hibernate.id.insert.InsertGeneratedIdentifierDelegate;
import org.hibernate.id.insert.InsertReturningDelegate;

/**
 * An {@link InDatabaseGenerator} that handles {@code IDENTITY}/"autoincrement" columns
 * on those databases which support them.
 * <p>
 * Delegates to the {@link org.hibernate.dialect.identity.IdentityColumnSupport} provided
 * by the {@linkplain Dialect#getIdentityColumnSupport() dialect}.
 * <p>
 * The actual work involved in retrieving the primary key value is the job of a
 * {@link org.hibernate.id.insert.InsertGeneratedIdentifierDelegate}, either:
 * <ul>
 * <li>a {@link org.hibernate.id.insert.GetGeneratedKeysDelegate},
 * <li>an {@link org.hibernate.id.insert.InsertReturningDelegate}, or a
 * <li>a {@link org.hibernate.id.insert.BasicSelectingDelegate}.
 * </ul>
 *
 * @see org.hibernate.dialect.identity.IdentityColumnSupport
 * @see org.hibernate.id.insert.InsertGeneratedIdentifierDelegate
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

	@Override
	public InsertGeneratedIdentifierDelegate getGeneratedIdentifierDelegate(PostInsertIdentityPersister persister) {
		Dialect dialect = persister.getFactory().getJdbcServices().getDialect();
		if ( persister.getFactory().getSessionFactoryOptions().isGetGeneratedKeysEnabled() ) {
			return dialect.getIdentityColumnSupport().buildGetGeneratedKeysDelegate( persister, dialect );
		}
		else if ( dialect.getIdentityColumnSupport().supportsInsertSelectIdentity() ) {
			return new InsertReturningDelegate( persister, dialect );
		}
		else {
			return new BasicSelectingDelegate( persister, dialect );
		}
	}
}
