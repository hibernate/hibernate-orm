/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import org.hibernate.dialect.Dialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tuple.GenerationTiming;
import org.hibernate.tuple.InDatabaseGenerator;
import org.hibernate.type.Type;

import java.util.Properties;

import static org.hibernate.tuple.GenerationTiming.INSERT;

/**
 * @author Gavin King
 */
public interface PostInsertIdentifierGenerator extends InDatabaseGenerator, Configurable {

	@Override
	default GenerationTiming getGenerationTiming() {
		return INSERT;
	}

	@Override
	default boolean writePropertyValue() {
		return false;
	}

	@Override
	default boolean referenceColumnsInSql(Dialect dialect) {
		return dialect.getIdentityColumnSupport().hasIdentityInsertKeyword();
	}

	@Override
	default String[] getReferencedColumnValues(Dialect dialect) {
		return new String[] { dialect.getIdentityColumnSupport().getIdentityInsertString() };
	}

	@Override
	default void configure(Type type, Properties params, ServiceRegistry serviceRegistry) {}

}
