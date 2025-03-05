/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph;

import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaType;

public class UnfetchedBasicPartResultAssembler<J>  implements DomainResultAssembler<J> {

	private final JavaType<J> javaType;

	public UnfetchedBasicPartResultAssembler(JavaType<J> javaType) {
		this.javaType = javaType;
	}

	@Override
	public J assemble(RowProcessingState rowProcessingState) {
		return null;
	}

	@Override
	public JavaType<J> getAssembledJavaType() {
		return javaType;
	}

}
