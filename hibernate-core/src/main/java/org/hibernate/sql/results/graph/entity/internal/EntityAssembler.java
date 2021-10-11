/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class EntityAssembler implements DomainResultAssembler {
	private final JavaType javaTypeDescriptor;
	private final EntityInitializer initializer;
	private EntityInitializer replacedInitializer;

	public EntityAssembler(
			JavaType javaTypeDescriptor,
			EntityInitializer initializer) {
		this.javaTypeDescriptor = javaTypeDescriptor;
		this.initializer = initializer;
	}

	@Override
	public JavaType getAssembledJavaTypeDescriptor() {
		return javaTypeDescriptor;
	}

	@Override
	public Object assemble(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options) {
		// Ensure that the instance really is initialized
		// This is important for key-many-to-ones that are part of a collection key fk,
		// as the instance is needed for resolveKey before initializing the instance in RowReader
		initializer.resolveInstance( rowProcessingState );
		return initializer.getEntityInstance();
	}
}
