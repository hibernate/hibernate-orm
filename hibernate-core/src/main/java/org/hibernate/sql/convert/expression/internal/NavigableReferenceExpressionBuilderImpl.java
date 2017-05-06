/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.convert.expression.internal;

import org.hibernate.persister.collection.spi.CollectionPersister;
import org.hibernate.persister.common.internal.SingularPersistentAttributeEntity;
import org.hibernate.persister.common.spi.SingularPersistentAttribute;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.query.sqm.tree.expression.domain.SqmCollectionElementReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmPluralAttributeReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReference;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.tree.expression.domain.ColumnBindingSource;
import org.hibernate.sql.tree.expression.domain.CompositeColumnBindingSource;
import org.hibernate.sql.tree.expression.domain.EntityReferenceExpression;
import org.hibernate.sql.tree.expression.domain.NavigableReferenceExpression;
import org.hibernate.sql.tree.expression.domain.PluralAttributeElementReferenceExpression;
import org.hibernate.sql.tree.expression.domain.SingularAttributeReferenceExpression;
import org.hibernate.sql.tree.from.CollectionTableGroup;
import org.hibernate.sql.tree.from.TableGroup;
import org.hibernate.sql.convert.expression.spi.NavigableReferenceExpressionBuilder;

/**
 * @author Steve Ebersole
 */
public class NavigableReferenceExpressionBuilderImpl implements NavigableReferenceExpressionBuilder {
	private final boolean shallow;

	public NavigableReferenceExpressionBuilderImpl(boolean shallow) {
		this.shallow = shallow;
	}

	@Override
	public boolean isShallow() {
		return shallow;
	}

	@Override
	public NavigableReferenceExpression buildEntityExpression(
			BuildingContext buildingContext,
			ColumnBindingSource columnBindingSource,
			EntityPersister entityPersister,
			NavigablePath navigablePath) {
		return new EntityReferenceExpression(
				columnBindingSource,
				entityPersister,
				navigablePath,
				shallow
		);
	}

	@Override
	public NavigableReferenceExpression buildSingularAttributeExpression(
			BuildingContext buildingContext,
			SqmSingularAttributeReference singularAttributeBinding) {
		final NavigablePath propertyPath = singularAttributeBinding.getNavigablePath();

		if ( singularAttributeBinding.getReferencedNavigable() instanceof SingularPersistentAttributeEntity ) {
			final SingularPersistentAttributeEntity entityTypedAttribute = (SingularPersistentAttributeEntity) singularAttributeBinding.getReferencedNavigable();
			if ( singularAttributeBinding.getFromElement() == null ) {
				throw new IllegalStateException( "entity-typed SingularAttributeBinding did not contain SqmFrom element" );
			}
			return new EntityReferenceExpression(
					buildingContext.getFromClauseIndex().findResolvedTableGroup( singularAttributeBinding.getFromElement() ),
					entityTypedAttribute.getAssociatedEntityPersister(),
					propertyPath,
					shallow
			);
		}
		else {
			final SingularPersistentAttribute singularAttribute = singularAttributeBinding.getReferencedNavigable();
			final TableGroup tableGroup = buildingContext.getFromClauseIndex().findResolvedTableGroup( singularAttributeBinding.getLhs() );

			if ( singularAttributeBinding.getFromElement() == null ) {
				return new SingularAttributeReferenceExpression(
						tableGroup,
						singularAttribute,
						propertyPath
				);
			}
			else {
				return new SingularAttributeReferenceExpression(
						new CompositeColumnBindingSource(
								buildingContext.getFromClauseIndex().findResolvedTableGroup( singularAttributeBinding.getFromElement() ),
								tableGroup
						),
						singularAttribute,
						propertyPath
				);
			}
		}
	}

	@Override
	public NavigableReferenceExpression buildPluralAttributeExpression(
			BuildingContext buildingContext,
			SqmPluralAttributeReference pluralAttributeReference) {

		CollectionTableGroup tableGroup = null;
		if ( pluralAttributeReference.getFromElement() != null ) {
			tableGroup = (CollectionTableGroup) buildingContext.getFromClauseIndex().findResolvedTableGroup( pluralAttributeReference.getFromElement() );
		}

		if ( tableGroup == null ) {
			throw new IllegalStateException( "From-element (join) for plural-attribute is not resolved" );
		}


		throw new NotYetImplementedException( "resolving AttributeBinding for plural-attributes" );
	}

	@Override
	public NavigableReferenceExpression buildPluralAttributeElementReferenceExpression(
			SqmCollectionElementReference collectionElementReference,
			TableGroup resolvedTableGroup,
			NavigablePath navigablePath) {
		final CollectionPersister collectionPersister = collectionElementReference.getSourceReference()
				.getReferencedNavigable()
				.getCollectionPersister();
		return new PluralAttributeElementReferenceExpression(
				collectionPersister,
				resolvedTableGroup,
				navigablePath,
				isShallow()
		);
	}
}
