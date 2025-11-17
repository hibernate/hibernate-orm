/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph;

import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Christian Beikov
 */
public class UnfetchedResultAssembler<J>  implements DomainResultAssembler<J> {

	private final JavaType<J> javaType;

	public UnfetchedResultAssembler(JavaType<J> javaType) {
		this.javaType = javaType;
	}

	@Override
	public J assemble(RowProcessingState rowProcessingState) {
		return (J) LazyPropertyInitializer.UNFETCHED_PROPERTY;
	}

	@Override
	public JavaType<J> getAssembledJavaType() {
		return javaType;
	}

}
