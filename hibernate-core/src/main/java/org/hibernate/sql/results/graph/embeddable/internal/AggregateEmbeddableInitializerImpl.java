/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.embeddable.internal;

import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.embeddable.EmbeddableInitializer;
import org.hibernate.sql.results.graph.embeddable.EmbeddableResultGraphNode;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

/**
 * An initializer for an embeddable that is mapped as aggregate e.g. STRUCT, JSON or XML.
 * The aggregate selection reads an Object[] from JDBC which serves as data for the nested {@link DomainResultAssembler}.
 * This class exposes the Object[] of the aggregate to the nested assemblers through a wrapping {@link RowProcessingState}.
 */
public class AggregateEmbeddableInitializerImpl extends EmbeddableInitializerImpl {

	private final int[] aggregateValuesArrayPositions;

	public AggregateEmbeddableInitializerImpl(
			EmbeddableResultGraphNode resultDescriptor,
			BasicFetch<?> discriminatorFetch,
			InitializerParent<?> parent,
			AssemblerCreationState creationState,
			boolean isResultInitializer,
			SqlSelection structSelection) {
		super( resultDescriptor, discriminatorFetch, parent, creationState, isResultInitializer );
		this.aggregateValuesArrayPositions = determineAggregateValuesArrayPositions(
				parent,
				structSelection
		);
		final DomainResultAssembler<?>[][] assemblers = super.createAssemblers(
				resultDescriptor,
				creationState,
				resultDescriptor.getReferencedMappingType()
		);
		System.arraycopy( assemblers, 0, this.assemblers, 0, assemblers.length );
		final Initializer<?>[][] initializers = createInitializers( assemblers );
		System.arraycopy( initializers, 0, this.subInitializers, 0, initializers.length );
	}

	@Override
	public void startLoading(RowProcessingState rowProcessingState) {
		super.startLoading( NestedRowProcessingState.wrap( this, rowProcessingState ) );
	}

	@Override
	protected DomainResultAssembler<?>[][] createAssemblers(
			EmbeddableResultGraphNode resultDescriptor,
			AssemblerCreationState creationState,
			EmbeddableMappingType embeddableTypeDescriptor) {
		// Return just the assemblers array here without elements,
		// as we initialize the array in the constructor after the aggregateValuesArrayPositions is set
		return new DomainResultAssembler[embeddableTypeDescriptor.isPolymorphic() ? embeddableTypeDescriptor.getConcreteEmbeddableTypes().size() : 1][];
	}

	public int[] getAggregateValuesArrayPositions() {
		return aggregateValuesArrayPositions;
	}

	public Object[] getJdbcValues(RowProcessingState processingState) {
		final int[] aggregateValuesArrayPositions = getAggregateValuesArrayPositions();
		Object[] jdbcValue = (Object[]) processingState.getJdbcValue( aggregateValuesArrayPositions[0] );
		for ( int i = 1; i < aggregateValuesArrayPositions.length; i++ ) {
			if ( jdbcValue == null ) {
				break;
			}
			jdbcValue = (Object[]) jdbcValue[aggregateValuesArrayPositions[i]];
		}
		return jdbcValue;
	}

	static int[] determineAggregateValuesArrayPositions(
			InitializerParent<?> parent,
			SqlSelection structSelection) {
		if ( parent instanceof AggregateEmbeddableInitializerImpl ) {
			final int[] parentAggregateValuesArrayPositions = ( (AggregateEmbeddableInitializerImpl) parent ).getAggregateValuesArrayPositions();
			final int[] aggregateValuesArrayPositions = new int[parentAggregateValuesArrayPositions.length + 1];
			System.arraycopy(
					parentAggregateValuesArrayPositions,
					0,
					aggregateValuesArrayPositions,
					0,
					parentAggregateValuesArrayPositions.length
			);
			aggregateValuesArrayPositions[aggregateValuesArrayPositions.length - 1] = structSelection.getValuesArrayPosition();
			return aggregateValuesArrayPositions;
		}
		else if ( parent instanceof EmbeddableInitializer ) {
			return determineAggregateValuesArrayPositions( parent.getParent(), structSelection );
		}
		return new int[] { structSelection.getValuesArrayPosition() };
	}

}
