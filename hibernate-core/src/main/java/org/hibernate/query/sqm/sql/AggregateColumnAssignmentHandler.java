/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.sql;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.dialect.aggregate.AggregateSupport;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.tree.expression.AggregateColumnWriteExpression;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Handler for assignments to sub-columns of an aggregate column, which require a special write expression.
 * It receives a callback for all assignments and records the positions of the assignments that belong to
 * an aggregate where {@link EmbeddableMappingType#requiresAggregateColumnWriter()} is <code>true</code>.
 * In a single post-processing step it replaces individual assignments with a single aggregate assignment.
 */
public class AggregateColumnAssignmentHandler {
	private final TypeConfiguration typeConfiguration;
	private final AggregateSupport aggregateSupport;
	private final HashMap<SelectablePath, EmbeddableMappingType> rootAggregates;
	private final int assignmentCount;
	private final LinkedHashMap<EmbeddableMappingType, BitSet> aggregateAssignmentPositions;

	private AggregateColumnAssignmentHandler(EntityPersister entityDescriptor, int assignmentCount) {
		final HashMap<SelectablePath, EmbeddableMappingType> rootAggregates = new HashMap<>();
		collectRootAggregates( rootAggregates, entityDescriptor );
		this.typeConfiguration = entityDescriptor.getFactory().getTypeConfiguration();
		this.aggregateSupport = entityDescriptor.getFactory().getJdbcServices()
				.getDialect()
				.getAggregateSupport();
		this.rootAggregates = rootAggregates;
		this.assignmentCount = assignmentCount;
		this.aggregateAssignmentPositions = new LinkedHashMap<>();
	}

	private static void collectRootAggregates(
			HashMap<SelectablePath, EmbeddableMappingType> rootAggregates,
			ManagedMappingType mappingType) {
		final int numberOfAttributeMappings = mappingType.getNumberOfAttributeMappings();
		for ( int i = 0; i < numberOfAttributeMappings; i++ ) {
			final AttributeMapping attributeMapping = mappingType.getAttributeMapping( i );
			final MappingType mappedType = attributeMapping.getMappedType();
			if ( mappedType instanceof EmbeddableMappingType embeddableMappingType ) {
				final SelectableMapping aggregateMapping = embeddableMappingType.getAggregateMapping();
				if ( aggregateMapping == null ) {
					collectRootAggregates( rootAggregates, embeddableMappingType );
				}
				else {
					rootAggregates.put( aggregateMapping.getSelectablePath(), embeddableMappingType );
				}
			}
		}
	}

	static AggregateColumnAssignmentHandler forEntityDescriptor(EntityPersister entityDescriptor, int size) {
		if ( entityDescriptor.anyRequiresAggregateColumnWriter() ) {
			return new AggregateColumnAssignmentHandler( entityDescriptor, size );
		}
		return null;
	}

	public void addAssignment(int position, ColumnReference columnReference) {
		final EmbeddableMappingType aggregateType = findAggregateToCombine( columnReference.getSelectablePath() );
		if ( aggregateType == null ) {
			// this means that no aggregate in that selectable path requires combination
			return;
		}
		assert aggregateType.requiresAggregateColumnWriter();
		BitSet positions = aggregateAssignmentPositions.get( aggregateType );
		if ( positions == null ) {
			positions = new BitSet( assignmentCount );
			aggregateAssignmentPositions.put( aggregateType, positions );
		}
		positions.set( position );
	}

	public void aggregateAssignments(ArrayList<Assignment> assignments) {
		final var aggregateAssignments = new ArrayList<Assignment>( aggregateAssignmentPositions.size() );
		final BitSet consumedPositions = new BitSet( assignmentCount );
		for ( Map.Entry<EmbeddableMappingType, BitSet> entry : aggregateAssignmentPositions.entrySet() ) {
			final EmbeddableMappingType aggregateType = entry.getKey();
			final BitSet assignmentPositions = entry.getValue();
			final int assignmentsPositionCount = assignmentPositions.cardinality();
			final SelectableMapping aggregateMapping = aggregateType.getAggregateMapping();
			final ColumnReference aggregateColumnReference = new ColumnReference(
					assignments.get( assignmentPositions.nextSetBit( 0 ) ).getAssignable().getColumnReferences()
							.get( 0 )
							.getQualifier(),
					aggregateMapping
			);
			final SelectableMapping[] columnMappings = new SelectableMapping[assignmentsPositionCount];
			final Expression[] valueExpressions = new Expression[assignmentsPositionCount];
			for ( int assignmentPosition = assignmentPositions.nextSetBit( 0 ), i = 0;
					assignmentPosition >= 0;
					assignmentPosition = assignmentPositions.nextSetBit( assignmentPosition + 1 ), i++ ) {
				final Assignment assignment = assignments.get( assignmentPosition );
				final List<ColumnReference> columnReferences = assignment.getAssignable().getColumnReferences();
				assert columnReferences.size() == 1;
				columnMappings[i] = findSelectable( aggregateType, columnReferences.get( 0 ) );
				valueExpressions[i] = assignment.getAssignedValue();
				consumedPositions.set( assignmentPosition );
			}

			final AggregateSupport.WriteExpressionRenderer writeExpression = aggregateSupport.aggregateCustomWriteExpressionRenderer(
					aggregateMapping,
					columnMappings,
					typeConfiguration
			);
			aggregateAssignments.add(
					new Assignment(
							aggregateColumnReference,
							new AggregateColumnWriteExpression(
									aggregateColumnReference,
									writeExpression,
									columnMappings,
									valueExpressions
							)
					)
			);
		}
		if ( aggregateAssignments.isEmpty() ) {
			return;
		}

		// Replace the consumed positions with the respective aggregate assignments
		//
		//                   aggregateEndIndex --   -- endIndex
		//                                      |   |
		//                 aggregateStartIndex  |   |
		//                          |           |   |
		//  assignmentEndIndex --   |           |   |
		//                      v   v           v   v
		//                ------------------------------------------------------------------------
		//                | 0 | 1 | 2 | 3 | 4 | 5 | 6 |
		//                ------------------------------------------------------------------------
		//                          ^-----------^
		//                           aggregated
		//
		// The following will first move the element at index 6 (endIndex) to index 3 (aggregateStartIndex + 1).
		// Then removes elements from right to left starting at index 5 (aggregateEndIndex)
		// and stopping at index 4 (aggregateEndIndex - (endIndex - aggregateEndIndex)).
		// Finally, it sets index 2 (aggregateStartIndex) to the last aggregate assignment.
		int endIndex = assignmentCount - 1;
		int aggregateAssignmentIndex = aggregateAssignments.size() - 1;
		int aggregateEndIndex = consumedPositions.previousSetBit( endIndex );
		do {
			final int assignmentEndIndex = consumedPositions.previousClearBit( aggregateEndIndex );
			final int aggregateStartIndex = assignmentEndIndex + 1;
			final int regularAssignments = endIndex - aggregateEndIndex;
			// Move over the regular assignments from right to left to aggregateStartIndex + 1
			for ( int i = regularAssignments; i != 0; i-- ) {
				assignments.set( aggregateStartIndex + i, assignments.remove( aggregateEndIndex + i ) );
			}
			// Remove other consumed positions
			final int newEnd = aggregateStartIndex + regularAssignments + 1;
			for ( int i = aggregateEndIndex - regularAssignments; i >= newEnd; i-- ) {
				assignments.remove( i );
			}
			// Replace the first of the consumed positions with the actual aggregate assignment
			assignments.set( aggregateStartIndex, aggregateAssignments.get( aggregateAssignmentIndex ) );

			endIndex = assignmentEndIndex;
			aggregateEndIndex = consumedPositions.previousSetBit( endIndex );
			aggregateAssignmentIndex--;
		} while ( aggregateEndIndex != -1 );
		assert aggregateAssignmentIndex == -1;
	}

	private SelectableMapping findSelectable(EmbeddableMappingType aggregateType, ColumnReference columnReference) {
		final SelectablePath aggregateSelectablePath = aggregateType.getAggregateMapping().getSelectablePath();
		final SelectablePath[] relativeSelectablePaths = columnReference.getSelectablePath()
				.relativize( aggregateSelectablePath );
		final int end = relativeSelectablePaths.length - 1;
		for ( int i = 0; i < end; i++ ) {
			final SelectableMapping selectable = aggregateType.getJdbcValueSelectable(
					aggregateType.getSelectableIndex( relativeSelectablePaths[i].getSelectableName() )
			);
			aggregateType = ( (AggregateJdbcType) selectable.getJdbcMapping().getJdbcType() )
					.getEmbeddableMappingType();
		}
		return aggregateType.getJdbcValueSelectable(
				aggregateType.getSelectableIndex( relativeSelectablePaths[end].getSelectableName() )
		);
	}

	private EmbeddableMappingType findAggregateToCombine(SelectablePath selectablePath) {
		final SelectablePath[] parts = selectablePath.getParts();
		for ( int i = parts.length - 2; i >= 0; i-- ) {
			final EmbeddableMappingType embeddableMappingType = rootAggregates.get( parts[i] );
			if ( embeddableMappingType != null && embeddableMappingType.requiresAggregateColumnWriter() ) {
				return embeddableMappingType;
			}
		}
		return null;
	}
}
