/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.metamodel.mapping.Association;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.entity.UnionSubclassEntityPersister;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * @author Steve Ebersole
 */
public class MappingModelHelper {

	private MappingModelHelper() {
		// disallow direct instantiation
	}
//
//	public static Expression buildColumnReferenceExpression(
//			ModelPart modelPart,
//			SqlExpressionResolver sqlExpressionResolver) {
//		return buildColumnReferenceExpression( null, modelPart, sqlExpressionResolver );
//	}
//
//	public static Expression buildColumnReferenceExpression(
//			TableGroup tableGroup,
//			ModelPart modelPart,
//			SqlExpressionResolver sqlExpressionResolver) {
//		final int jdbcTypeCount = modelPart.getJdbcTypeCount();
//
//		if ( modelPart instanceof EmbeddableValuedModelPart ) {
//			final List<ColumnReference> columnReferences = new ArrayList<>( jdbcTypeCount );
//			modelPart.forEachSelectable(
//					(columnIndex, selection) -> {
//						final ColumnReference colRef;
//						final String qualifier;
//						if ( tableGroup == null ) {
//							qualifier = selection.getContainingTableExpression();
//						}
//						else {
//							qualifier = tableGroup.resolveTableReference( selection.getContainingTableExpression() ).getIdentificationVariable();
//						}
//						if ( sqlExpressionResolver == null ) {
//							colRef = new ColumnReference(
//									qualifier,
//									selection
//							);
//						}
//						else {
//							colRef = (ColumnReference) sqlExpressionResolver.resolveSqlExpression(
//									createColumnReferenceKey( qualifier, selection ),
//									sqlAstProcessingState -> new ColumnReference(
//											qualifier,
//											selection
//									)
//							);
//						}
//						columnReferences.add( colRef );
//					}
//			);
//			return new SqlTuple( columnReferences, modelPart );
//		}
//		else {
//			assert modelPart instanceof BasicValuedModelPart;
//			final BasicValuedModelPart basicPart = (BasicValuedModelPart) modelPart;
//			final String qualifier;
//			if ( tableGroup == null ) {
//				qualifier = basicPart.getContainingTableExpression();
//			}
//			else {
//				qualifier = tableGroup.resolveTableReference( basicPart.getContainingTableExpression() ).getIdentificationVariable();
//			}
//			if ( sqlExpressionResolver == null ) {
//				return new ColumnReference(
//						qualifier,
//						basicPart
//				);
//			}
//			else {
//				return sqlExpressionResolver.resolveSqlExpression(
//						createColumnReferenceKey( qualifier, basicPart ),
//						sqlAstProcessingState -> new ColumnReference(
//								qualifier,
//								basicPart
//						)
//				);
//			}
//		}
//	}
//
	public static boolean isCompatibleModelPart(ModelPart attribute1, ModelPart attribute2) {
		if ( attribute1 == attribute2 ) {
			return true;
		}
		if ( attribute1.getClass() != attribute2.getClass() || attribute1.getJavaType() != attribute2.getJavaType() ) {
			return false;
		}
		if ( attribute1 instanceof Association association1 ) {
			final var association2 = (Association) attribute2;
			return association1.getForeignKeyDescriptor().getAssociationKey().equals(
					association2.getForeignKeyDescriptor().getAssociationKey()
			);
		}
		else if ( attribute1 instanceof PluralAttributeMapping plural1 ) {
			final var plural2 = (PluralAttributeMapping) attribute2;
			final var element1 = plural1.getElementDescriptor();
			final var element2 = plural2.getElementDescriptor();
			final var index1 = plural1.getIndexDescriptor();
			final var index2 = plural2.getIndexDescriptor();
			return plural1.getKeyDescriptor().getAssociationKey()
						.equals( plural2.getKeyDescriptor().getAssociationKey() )
				&& ( index1 == null && index2 == null || isCompatibleModelPart( index1, index2 ) )
				&& isCompatibleModelPart( element1, element2 );
		}
		else if ( attribute1 instanceof EmbeddableValuedModelPart embedded1 ) {
			final var embedded2 = (EmbeddableValuedModelPart) attribute2;
			final var embeddableTypeDescriptor1 = embedded1.getEmbeddableTypeDescriptor();
			final var embeddableTypeDescriptor2 = embedded2.getEmbeddableTypeDescriptor();
			final int numberOfAttributeMappings = embeddableTypeDescriptor1.getNumberOfAttributeMappings();
			if ( numberOfAttributeMappings != embeddableTypeDescriptor2.getNumberOfAttributeMappings() ) {
				return false;
			}
			for ( int i = 0; i < numberOfAttributeMappings; i++ ) {
				if ( !isCompatibleModelPart(
						embeddableTypeDescriptor1.getAttributeMapping( i ),
						embeddableTypeDescriptor2.getAttributeMapping( i )
				) ) {
					return false;
				}
			}
			return true;
		}
		else {
			final var basic1 = attribute1.asBasicValuedModelPart();
			if ( basic1 != null ) {
				final var basic2 = castNonNull( attribute2.asBasicValuedModelPart() );
				if ( !basic1.getSelectionExpression().equals( basic2.getSelectionExpression() ) ) {
					return false;
				}
				if ( basic1.getContainingTableExpression().equals( basic2.getContainingTableExpression() ) ) {
					return true;
				}
				// For union subclass mappings we also consider mappings compatible that just match the selection expression,
				// because we match up columns of disjoint union subclass types by column name
				return attribute1.findContainingEntityMapping()
						.getEntityPersister() instanceof UnionSubclassEntityPersister;
			}
		}
		return false;
	}
}
