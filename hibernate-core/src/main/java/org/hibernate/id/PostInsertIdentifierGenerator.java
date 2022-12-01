/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.id.insert.InsertGeneratedIdentifierDelegate;
import org.hibernate.tuple.GenerationTiming;
import org.hibernate.tuple.InDatabaseGenerator;

import static org.hibernate.tuple.GenerationTiming.INSERT;

/**
 * @author Gavin King
 */
public interface PostInsertIdentifierGenerator extends InDatabaseGenerator, Configurable {
	InsertGeneratedIdentifierDelegate getInsertGeneratedIdentifierDelegate(
			PostInsertIdentityPersister persister,
			Dialect dialect,
			boolean isGetGeneratedKeysEnabled) throws HibernateException;

	@Override
	default GenerationTiming getGenerationTiming() {
		return INSERT;
	}

	@Override
	default boolean writePropertyValue() {
		return false;
	}
}
