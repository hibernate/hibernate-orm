/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.entity;

import java.util.function.Consumer;

import org.hibernate.metamodel.model.domain.spi.DiscriminatorDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.tree.spi.expression.domain.EntityValuedNavigableReference;
import org.hibernate.sql.results.spi.AssemblerCreationContext;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.Initializer;
import org.hibernate.sql.results.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.spi.RowProcessingState;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class DiscriminatorDomainResult implements DomainResult {
	private final DiscriminatorDescriptor discriminatorDescriptor;

	private final String resultVariable;
	private final SqlSelection sqlSelection;

	private final NavigablePath navigablePath;

	public <J, O> DiscriminatorDomainResult(
			DiscriminatorDescriptor discriminatorDescriptor,
			SqlSelection sqlSelection,
			NavigablePath navigablePath,
			String resultVariable) {
		this.discriminatorDescriptor = discriminatorDescriptor;
		this.sqlSelection = sqlSelection;
		this.navigablePath = navigablePath;
		this.resultVariable = resultVariable;
	}


	@Override
	public String getResultVariable() {
		return resultVariable;
	}

	@Override
	public BasicJavaDescriptor<?> getJavaTypeDescriptor() {
		return discriminatorDescriptor.getJavaTypeDescriptor();
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public DomainResultAssembler createResultAssembler(
			Consumer<Initializer> initializerCollector,
			AssemblerCreationState creationOptions,
			AssemblerCreationContext creationContext) {
		return new DomainResultAssemblerImpl(
				discriminatorDescriptor,
				sqlSelection
		);
	}


	private static class DomainResultAssemblerImpl implements DomainResultAssembler {
		private final DiscriminatorDescriptor discriminatorDescriptor;
		private final SqlSelection sqlSelection;

		public DomainResultAssemblerImpl(
				DiscriminatorDescriptor discriminatorDescriptor,
				SqlSelection sqlSelection) {
			this.discriminatorDescriptor = discriminatorDescriptor;
			this.sqlSelection = sqlSelection;
		}

		@Override
		public Object assemble(
				RowProcessingState rowProcessingState,
				JdbcValuesSourceProcessingOptions options) {
			final Object jdbcValue = rowProcessingState.getJdbcValue( sqlSelection );
			final String entityName = discriminatorDescriptor.getDiscriminatorMappings()
					.discriminatorValueToEntityName( jdbcValue );
			final EntityTypeDescriptor<Object> entityDescriptor = rowProcessingState.getSession()
					.getFactory()
					.getMetamodel()
					.findEntityDescriptor( entityName );
			return entityDescriptor.getJavaType();
		}

		@Override
		public BasicJavaDescriptor<?> getJavaTypeDescriptor() {
			return discriminatorDescriptor.getJavaTypeDescriptor();
		}
	}
}
