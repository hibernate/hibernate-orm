/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.id.insert.BasicSelectingDelegate;
import org.hibernate.id.insert.InsertGeneratedIdentifierDelegate;
import org.hibernate.id.insert.InsertReturningDelegate;

/**
 * A generator for use with ANSI-SQL IDENTITY columns used as the primary key.
 * The IdentityGenerator for autoincrement/identity key generation.
 * <p>
 * Indicates to the {@code Session} that identity (ie. identity/autoincrement
 * column) key generation should be used.
 *
 * @implNote Most of the functionality of this generator is delegated to
 * 		{@link InsertGeneratedIdentifierDelegate} (see
 * 		{@link #getInsertGeneratedIdentifierDelegate}).
 *
 * @author Christoph Sturm
 */
public class IdentityGenerator extends AbstractPostInsertGenerator {

	@Override
	public InsertGeneratedIdentifierDelegate getInsertGeneratedIdentifierDelegate(
			PostInsertIdentityPersister persister,
			Dialect dialect,
			boolean useGetGeneratedKeys) throws HibernateException {
		if ( useGetGeneratedKeys ) {
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
