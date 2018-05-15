/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.instantiation;

import org.hibernate.sql.results.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Specialized QueryResultAssembler for use as a "reader" for dynamic-
 * instantiation arguments.
 *
 * @author Steve Ebersole
 */
public class ArgumentReader implements DomainResultAssembler {
	private final DomainResultAssembler delegateAssembler;
	private final String alias;

	public ArgumentReader(DomainResultAssembler delegateAssembler, String alias) {
		this.delegateAssembler = delegateAssembler;
		this.alias = alias;
	}

	public String getAlias() {
		return alias;
	}

	@Override
	public Object assemble(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options) {
		return delegateAssembler.assemble( rowProcessingState, options );
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return delegateAssembler.getJavaTypeDescriptor();
	}
}
