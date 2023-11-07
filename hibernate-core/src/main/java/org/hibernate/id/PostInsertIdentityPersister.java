/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import org.hibernate.persister.entity.EntityPersister;

/**
 * A persister that may have an identity assigned by execution of
 * a SQL {@code INSERT}.
 *
 * @author Gavin King
 * @deprecated Use {@link EntityPersister} instead.
 */
@Deprecated( forRemoval = true, since = "6.5" )
public interface PostInsertIdentityPersister extends EntityPersister {
	@Override
	String getIdentitySelectString();

	@Override
	String[] getIdentifierColumnNames();

	@Override
	String getSelectByUniqueKeyString(String propertyName);

	@Override
	default String getSelectByUniqueKeyString(String[] propertyNames) {
		return EntityPersister.super.getSelectByUniqueKeyString( propertyNames );
	}

	@Override
	String[] getRootTableKeyColumnNames();
}
