/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression.instantiation;

import java.util.function.Consumer;

import org.hibernate.sql.results.internal.instantiation.ArgumentReader;
import org.hibernate.sql.results.spi.AssemblerCreationContext;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.Initializer;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class ArgumentDomainResult implements DomainResult {
	private final DomainResult realDomainResult;

	public ArgumentDomainResult(DomainResult realDomainResult) {
		this.realDomainResult = realDomainResult;
	}

	@Override
	public String getResultVariable() {
		return realDomainResult.getResultVariable();
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return realDomainResult.getJavaTypeDescriptor();
	}

	@Override
	public ArgumentReader createResultAssembler(
			Consumer<Initializer> initializerCollector,
			AssemblerCreationState creationOptions,
			AssemblerCreationContext creationContext) {
		return new ArgumentReader(
				realDomainResult.createResultAssembler( initializerCollector, creationOptions, creationContext ),
				getResultVariable()
		);
	}
}
