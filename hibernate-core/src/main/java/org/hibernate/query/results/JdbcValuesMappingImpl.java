/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.LockMode;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.InitializerProducer;
import org.hibernate.sql.results.jdbc.internal.StandardJdbcValuesMapping;

/**
 * Implementation of JdbcValuesMapping for native / procedure queries
 *
 * @author Steve Ebersole
 */
public class JdbcValuesMappingImpl extends StandardJdbcValuesMapping {

	private final int rowSize;
	private final Map<String, LockMode> registeredLockModes;

	public JdbcValuesMappingImpl(
			List<SqlSelection> sqlSelections,
			List<DomainResult<?>> domainResults,
			int rowSize,
			Map<String, LockMode> registeredLockModes) {
		super( sqlSelections, domainResults );
		this.rowSize = rowSize;
		this.registeredLockModes = registeredLockModes;
	}

	@Override
	public int getRowSize() {
		return rowSize;
	}

	@Override
	public List<DomainResultAssembler<?>> resolveAssemblers(AssemblerCreationState creationState) {
		final AssemblerCreationState finalCreationState;
		if ( registeredLockModes == null ) {
			finalCreationState = creationState;
		}
		else {
			finalCreationState = new AssemblerCreationState() {
				@Override
				public LockMode determineEffectiveLockMode(String identificationVariable) {
					final LockMode lockMode = registeredLockModes.get( identificationVariable );
					if ( lockMode == null ) {
						return creationState.determineEffectiveLockMode( identificationVariable );
					}
					return lockMode;
				}

				@Override
				public Initializer resolveInitializer(
						NavigablePath navigablePath,
						ModelPart fetchedModelPart,
						Supplier<Initializer> producer) {
					return creationState.resolveInitializer( navigablePath, fetchedModelPart, producer );
				}

				@Override
				public <P extends FetchParent> Initializer resolveInitializer(
						P resultGraphNode,
						InitializerParent parent,
						InitializerProducer<P> producer) {
					return creationState.resolveInitializer(
							resultGraphNode,
							parent,
							(node, p, state) -> producer.createInitializer( node, p, this )
					);
				}

				@Override
				public SqlAstCreationContext getSqlAstCreationContext() {
					return creationState.getSqlAstCreationContext();
				}

				@Override
				public ExecutionContext getExecutionContext() {
					return creationState.getExecutionContext();
				}
			};
		}
		return super.resolveAssemblers( finalCreationState );
	}
}
