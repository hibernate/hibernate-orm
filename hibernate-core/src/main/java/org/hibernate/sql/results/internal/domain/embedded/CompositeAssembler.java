/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.embedded;

import org.hibernate.sql.results.spi.CompositeInitializer;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class CompositeAssembler implements DomainResultAssembler {
	private final CompositeInitializer initializer;


	public CompositeAssembler(CompositeInitializer initializer) {
		this.initializer = initializer;
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return initializer.getEmbeddedDescriptor().getJavaTypeDescriptor();
	}

	@Override
	public Object assemble(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options) {
		return initializer.getCompositeInstance();
	}

}
