/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.embeddable.internal;

import org.hibernate.sql.results.graph.embeddable.EmbeddableInitializer;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class EmbeddableAssembler implements DomainResultAssembler {
	protected final EmbeddableInitializer initializer;

	public EmbeddableAssembler(EmbeddableInitializer initializer) {
		this.initializer = initializer;
	}

	@Override
	public JavaType getAssembledJavaType() {
		return initializer.getInitializedPart().getJavaType();
	}

	@Override
	public Object assemble(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options) {
		initializer.resolveKey( rowProcessingState );
		initializer.resolveInstance( rowProcessingState );
		initializer.initializeInstance( rowProcessingState );
		return initializer.getCompositeInstance();
	}

	@Override
	public void resolveState(RowProcessingState rowProcessingState) {
		// use resolveState instead of initialize instance to avoid
		// unneeded embeddable instantiation and injection
		initializer.resolveState( rowProcessingState );
	}
}
