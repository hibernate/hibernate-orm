/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.entity;

import org.hibernate.sql.results.spi.EntityInitializer;
import org.hibernate.sql.results.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

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
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return javaTypeDescriptor;
	}

	@Override
	public Object assemble(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options) {
		return initializer.getEntityInstance();
	}
}
