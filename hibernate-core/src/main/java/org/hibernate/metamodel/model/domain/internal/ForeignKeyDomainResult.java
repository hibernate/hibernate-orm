/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.sql.results.spi.AssemblerCreationContext;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.Initializer;
import org.hibernate.sql.results.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.spi.RowProcessingState;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class ForeignKeyDomainResult implements DomainResult {
	private final JavaTypeDescriptor jtd;
	private final List<SqlSelection> sqlSelections;

	public ForeignKeyDomainResult(
			JavaTypeDescriptor jtd,
			List<SqlSelection> sqlSelections) {
		this.jtd = jtd;
		this.sqlSelections = sqlSelections;
	}

	@Override
	public String getResultVariable() {
		return null;
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return jtd;
	}

	@Override
	public DomainResultAssembler createResultAssembler(
			Consumer<Initializer> initializerCollector,
			AssemblerCreationState creationOptions,
			AssemblerCreationContext creationContext) {
		return new DomainResultAssembler() {
			@Override
			public Object assemble(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options) {
				if ( sqlSelections.size() == 1 ) {
					return rowProcessingState.getJdbcValue( sqlSelections.get( 0 ) );
				}
				else {
					throw new NotYetImplementedFor6Exception();
				}
			}

			@Override
			public JavaTypeDescriptor getJavaTypeDescriptor() {
				return jtd;
			}
		};
	}
}
