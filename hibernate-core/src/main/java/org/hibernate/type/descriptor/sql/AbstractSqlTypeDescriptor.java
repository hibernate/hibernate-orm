/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.sql;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.hibernate.type.descriptor.JdbcValueMapper;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
public abstract class AbstractSqlTypeDescriptor implements SqlTypeDescriptor {
	private final Map<JavaTypeDescriptor<?>,JdbcValueMapper<?>> valueMapperCache = new ConcurrentHashMap<>();

	protected <J> JdbcValueMapper<J> determineValueMapper(
			JavaTypeDescriptor<J> javaTypeDescriptor,
			Function<JavaTypeDescriptor<J>,JdbcValueMapper<J>> creator) {
		return (JdbcValueMapper<J>) valueMapperCache.computeIfAbsent(
				javaTypeDescriptor,
				javaTypeDescriptor1 -> creator.apply( javaTypeDescriptor )
		);
	}
}

