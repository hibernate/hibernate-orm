/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.internal;

import java.util.function.Consumer;

import org.hibernate.sql.results.internal.domain.basic.BasicResultAssembler;
import org.hibernate.sql.results.spi.AssemblerCreationContext;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.Initializer;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class ResolvedScalarDomainResult implements DomainResult {
	private final SqlSelection sqlSelection;
	private final String resultVariable;
	private final BasicJavaDescriptor javaTypeDescriptor;

	public ResolvedScalarDomainResult(
			SqlSelection sqlSelection,
			String resultVariable,
			BasicJavaDescriptor javaTypeDescriptor) {
		this.sqlSelection = sqlSelection;
		this.resultVariable = resultVariable;
		this.javaTypeDescriptor = javaTypeDescriptor;
	}

	@Override
	public String getResultVariable() {
		return resultVariable;
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return javaTypeDescriptor;
	}

	@Override
	public DomainResultAssembler createResultAssembler(
			Consumer<Initializer> initializerCollector,
			AssemblerCreationState creationOptions,
			AssemblerCreationContext creationContext) {
		return new BasicResultAssembler(
				sqlSelection,
				null,
				javaTypeDescriptor
		);
	}
}
