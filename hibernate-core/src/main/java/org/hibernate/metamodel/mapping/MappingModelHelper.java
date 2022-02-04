/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.UnionSubclassEntityPersister;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.TableGroup;

import static org.hibernate.sql.ast.spi.SqlExpressionResolver.createColumnReferenceKey;

/**
 * @author Steve Ebersole
 */
public class MappingModelHelper {

	private MappingModelHelper() {
		// disallow direct instantiation
	}

	public static Expression buildColumnReferenceExpression(
			ModelPart modelPart,
			SqlExpressionResolver sqlExpressionResolver,
			SessionFactoryImplementor sessionFactory) {
		return buildColumnReferenceExpression( null, modelPart, sqlExpressionResolver, sessionFactory );
	}

	public static Expression buildColumnReferenceExpression(
			TableGroup tableGroup,
			ModelPart modelPart,
			SqlExpressionResolver sqlExpressionResolver,
			SessionFactoryImplementor sessionFactory) {
		final int jdbcTypeCount = modelPart.getJdbcTypeCount();

		if ( modelPart instanceof EmbeddableValuedModelPart ) {
			final List<ColumnReference> columnReferences = new ArrayList<>( jdbcTypeCount );
			modelPart.forEachSelectable(
					(columnIndex, selection) -> {
						final ColumnReference colRef;
						final String qualifier;
						if ( tableGroup == null ) {
							qualifier = selection.getContainingTableExpression();
						}
						else {
							qualifier = tableGroup.resolveTableReference( selection.getContainingTableExpression() ).getIdentificationVariable();
						}
						if ( sqlExpressionResolver == null ) {
							colRef = new ColumnReference(
									qualifier,
									selection,
									sessionFactory
							);
						}
						else {
							colRef = (ColumnReference) sqlExpressionResolver.resolveSqlExpression(
									createColumnReferenceKey( qualifier, selection.getSelectionExpression() ),
									sqlAstProcessingState -> new ColumnReference(
											qualifier,
											selection,
											sessionFactory
									)
							);
						}
						columnReferences.add( colRef );
					}
			);
			return new SqlTuple( columnReferences, modelPart );
		}
		else {
			assert modelPart instanceof BasicValuedModelPart;
			final BasicValuedModelPart basicPart = (BasicValuedModelPart) modelPart;
			final String qualifier;
			if ( tableGroup == null ) {
				qualifier = basicPart.getContainingTableExpression();
			}
			else {
				qualifier = tableGroup.resolveTableReference( basicPart.getContainingTableExpression() ).getIdentificationVariable();
			}
			if ( sqlExpressionResolver == null ) {
				return new ColumnReference(
						qualifier,
						basicPart,
						sessionFactory
				);
			}
			else {
				return sqlExpressionResolver.resolveSqlExpression(
						createColumnReferenceKey( qualifier, basicPart.getSelectionExpression() ),
						sqlAstProcessingState -> new ColumnReference(
								qualifier,
								basicPart,
								sessionFactory
						)
				);
			}
		}
	}
	public static boolean isCompatibleModelPart(ModelPart attribute1, ModelPart attribute2) {
		if ( attribute1 == attribute2 ) {
			return true;
		}
		if ( attribute1.getClass() != attribute2.getClass() || attribute1.getJavaType() != attribute2.getJavaType() ) {
			return false;
		}
		if ( attribute1 instanceof Association ) {
			final Association association1 = (Association) attribute1;
			final Association association2 = (Association) attribute2;
			return association1.getForeignKeyDescriptor().getAssociationKey().equals(
					association2.getForeignKeyDescriptor().getAssociationKey()
			);
		}
		else if ( attribute1 instanceof PluralAttributeMapping ) {
			final PluralAttributeMapping plural1 = (PluralAttributeMapping) attribute1;
			final PluralAttributeMapping plural2 = (PluralAttributeMapping) attribute2;
			final CollectionPart element1 = plural1.getElementDescriptor();
			final CollectionPart element2 = plural2.getElementDescriptor();
			final CollectionPart index1 = plural1.getIndexDescriptor();
			final CollectionPart index2 = plural2.getIndexDescriptor();
			return plural1.getKeyDescriptor().getAssociationKey().equals(
					plural2.getKeyDescriptor().getAssociationKey()
			) && ( index1 == null && index2 == null || isCompatibleModelPart( index1, index2 ) )
					&& isCompatibleModelPart( element1, element2 );
		}
		else if ( attribute1 instanceof EmbeddableValuedModelPart ) {
			final EmbeddableValuedModelPart embedded1 = (EmbeddableValuedModelPart) attribute1;
			final EmbeddableValuedModelPart embedded2 = (EmbeddableValuedModelPart) attribute2;
			final List<AttributeMapping> attrs1 = embedded1.getEmbeddableTypeDescriptor().getAttributeMappings();
			final List<AttributeMapping> attrs2 = embedded2.getEmbeddableTypeDescriptor().getAttributeMappings();
			if ( attrs1.size() != attrs2.size() ) {
				return false;
			}
			for ( int i = 0; i < attrs1.size(); i++ ) {
				if ( !isCompatibleModelPart( attrs1.get( i ), attrs2.get( i ) ) ) {
					return false;
				}
			}
			return true;
		}
		else if ( attribute1 instanceof BasicValuedModelPart ) {
			final BasicValuedModelPart basic1 = (BasicValuedModelPart) attribute1;
			final BasicValuedModelPart basic2 = (BasicValuedModelPart) attribute2;
			if ( !basic1.getSelectionExpression().equals( basic2.getSelectionExpression() ) ) {
				return false;
			}
			if ( basic1.getContainingTableExpression().equals( basic2.getContainingTableExpression() ) ) {
				return true;
			}
			// For union subclass mappings we also consider mappings compatible that just match the selection expression,
			// because we match up columns of disjoint union subclass types by column name
			return attribute1.findContainingEntityMapping().getEntityPersister() instanceof UnionSubclassEntityPersister;
		}
		return false;
	}
}
