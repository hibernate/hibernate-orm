/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.identity;

import org.hibernate.Remove;
import org.hibernate.dialect.Dialect;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.id.insert.GetGeneratedKeysDelegate;

/**
 * @author Andrea Boriero
 *
 * @deprecated no longer used, use {@link GetGeneratedKeysDelegate} instead
 */
@Deprecated(forRemoval = true) @Remove
public class Oracle12cGetGeneratedKeysDelegate extends GetGeneratedKeysDelegate {
	public Oracle12cGetGeneratedKeysDelegate(PostInsertIdentityPersister persister, Dialect dialect) {
		super( persister, dialect, false );
	}
}
