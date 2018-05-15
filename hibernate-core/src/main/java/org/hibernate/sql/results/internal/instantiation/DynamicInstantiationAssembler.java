/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.instantiation;

import java.util.List;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.sql.results.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class DynamicInstantiationAssembler implements DomainResultAssembler {
	private final JavaTypeDescriptor javaTypeDescriptor;

	public DynamicInstantiationAssembler(JavaTypeDescriptor javaTypeDescriptor, List<SqlSelection> sqlSelections) {
		this.javaTypeDescriptor = javaTypeDescriptor;
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return javaTypeDescriptor;
	}

	@Override
	public Object assemble(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options) {
		throw new NotYetImplementedFor6Exception(  );
	}
}
