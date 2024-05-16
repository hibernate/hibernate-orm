/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.util.function.BiConsumer;

import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class EntityAssembler implements DomainResultAssembler {
	private final JavaType javaType;
	private final EntityInitializer initializer;

	public EntityAssembler(
			JavaType javaType,
			EntityInitializer initializer) {
		this.javaType = javaType;
		this.initializer = initializer;
	}

	@Override
	public JavaType getAssembledJavaType() {
		return javaType;
	}

	@Override
	public Object assemble(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options) {
		// Ensure that the instance really is initialized
		// This is important for key-many-to-ones that are part of a collection key fk,
		// as the instance is needed for resolveKey before initializing the instance in RowReader
		initializer.resolveInstance();
		return initializer.getEntityInstance();
	}

	@Override
	public EntityInitializer getInitializer() {
		return initializer;
	}

	@Override
	public void forEachResultAssembler(BiConsumer consumer, Object arg) {
		if ( initializer.isResultInitializer() ) {
			consumer.accept( initializer, arg );
		}
	}
}
