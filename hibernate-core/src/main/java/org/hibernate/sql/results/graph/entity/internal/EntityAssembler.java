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
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class EntityAssembler implements DomainResultAssembler {
	private final JavaTypeDescriptor javaTypeDescriptor;
	private final EntityInitializer initializer;

	public EntityAssembler(
			JavaTypeDescriptor javaTypeDescriptor,
			EntityInitializer initializer) {
		this.javaTypeDescriptor = javaTypeDescriptor;
		this.initializer = initializer;
	}

	@Override
	public JavaTypeDescriptor getAssembledJavaTypeDescriptor() {
		return javaTypeDescriptor;
	}

	@Override
	public Object assemble(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options) {
		return initializer.getEntityInstance();
	}
}
