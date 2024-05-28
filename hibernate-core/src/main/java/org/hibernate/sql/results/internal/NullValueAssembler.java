/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class NullValueAssembler<J> implements DomainResultAssembler<J> {
	private final JavaType<J> javaType;

	public NullValueAssembler(JavaType<J> javaType) {
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
