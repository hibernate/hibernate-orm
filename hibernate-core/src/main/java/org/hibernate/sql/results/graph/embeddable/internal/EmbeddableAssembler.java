/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.embeddable.internal;

import java.util.function.BiConsumer;

import org.hibernate.sql.results.graph.InitializerData;
import org.hibernate.sql.results.graph.embeddable.EmbeddableInitializer;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class EmbeddableAssembler implements DomainResultAssembler {
	protected final EmbeddableInitializer<InitializerData> initializer;

	public EmbeddableAssembler(EmbeddableInitializer<?> initializer) {
		this.initializer = (EmbeddableInitializer<InitializerData>) initializer;
	}

	@Override
	public JavaType getAssembledJavaType() {
		return initializer.getInitializedPart().getJavaType();
	}

	@Override
	public Object assemble(RowProcessingState rowProcessingState) {
		final InitializerData data = initializer.getData( rowProcessingState );
		initializer.resolveInstance( data );
		return initializer.getCompositeInstance( data );
	}

	@Override
	public void resolveState(RowProcessingState rowProcessingState) {
		// use resolveState instead of initialize instance to avoid
		// unneeded embeddable instantiation and injection
		initializer.resolveState( rowProcessingState );
	}

	@Override
	public EmbeddableInitializer<?> getInitializer() {
		return initializer;
	}

	@Override
	public void forEachResultAssembler(BiConsumer consumer, Object arg) {
		if ( initializer.isResultInitializer() ) {
			consumer.accept( initializer, arg );
		}
	}
}
