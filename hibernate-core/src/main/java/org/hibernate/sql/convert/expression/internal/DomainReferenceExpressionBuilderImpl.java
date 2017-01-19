/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.convert.expression.internal;

import org.hibernate.loader.PropertyPath;
import org.hibernate.persister.collection.spi.CollectionPersister;
import org.hibernate.persister.common.internal.PersisterHelper;
import org.hibernate.persister.common.internal.SingularPersistentAttributeEntity;
import org.hibernate.persister.common.spi.SingularOrmAttribute;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.expression.domain.ColumnBindingSource;
import org.hibernate.sql.ast.expression.domain.CompositeColumnBindingSource;
import org.hibernate.sql.ast.expression.domain.DomainReferenceExpression;
import org.hibernate.sql.ast.expression.domain.EntityReferenceExpression;
import org.hibernate.sql.ast.expression.domain.PluralAttributeElementReferenceExpression;
import org.hibernate.sql.ast.expression.domain.SingularAttributeReferenceExpression;
import org.hibernate.sql.ast.from.CollectionTableGroup;
import org.hibernate.sql.ast.from.TableGroup;
import org.hibernate.sql.convert.expression.spi.DomainReferenceExpressionBuilder;
import org.hibernate.sqm.query.expression.domain.PluralAttributeBinding;
import org.hibernate.sqm.query.expression.domain.PluralAttributeElementBinding;
import org.hibernate.sqm.query.expression.domain.SingularAttributeBinding;

/**
 * @author Steve Ebersole
 */
public class DomainReferenceExpressionBuilderImpl implements DomainReferenceExpressionBuilder {
	private final boolean shallow;

	public DomainReferenceExpressionBuilderImpl(boolean shallow) {
		this.shallow = shallow;
	}

	@Override
	public boolean isShallow() {
		return shallow;
	}

	@Override
	public DomainReferenceExpression buildEntityExpression(
			BuildingContext buildingContext,
			ColumnBindingSource columnBindingSource,
			EntityPersister entityPersister,
			PropertyPath propertyPath) {
		return new EntityReferenceExpression(
				columnBindingSource,
				entityPersister,
				propertyPath,
				shallow
		);
	}

	@Override
	public DomainReferenceExpression buildSingularAttributeExpression(
			BuildingContext buildingContext,
			SingularAttributeBinding singularAttributeBinding) {
		final PropertyPath propertyPath = PersisterHelper.convert( singularAttributeBinding.getPropertyPath() );

		if ( singularAttributeBinding.getAttribute() instanceof SingularPersistentAttributeEntity ) {
			final SingularPersistentAttributeEntity entityTypedAttribute = (SingularPersistentAttributeEntity) singularAttributeBinding.getAttribute();
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
			final SingularOrmAttribute singularAttribute = (SingularOrmAttribute) singularAttributeBinding.getAttribute();
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
	public DomainReferenceExpression buildPluralAttributeExpression(
			BuildingContext buildingContext,
			PluralAttributeBinding attributeBinding) {

		CollectionTableGroup tableGroup = null;
		if ( attributeBinding.getFromElement() != null ) {
			tableGroup = (CollectionTableGroup) buildingContext.getFromClauseIndex().findResolvedTableGroup( attributeBinding.getFromElement() );
		}

		if ( tableGroup == null ) {
			throw new IllegalStateException( "From-element (join) for plural-attribute is not resolved" );
		}


		throw new NotYetImplementedException( "resolving AttributeBinding for plural-attributes" );
	}

	@Override
	public DomainReferenceExpression buildPluralAttributeElementReferenceExpression(
			PluralAttributeElementBinding binding,
			TableGroup resolvedTableGroup,
			PropertyPath propertyPath) {
		final CollectionPersister collectionPersister = (CollectionPersister) binding.getPluralAttributeReference();
		return new PluralAttributeElementReferenceExpression(
				collectionPersister,
				resolvedTableGroup,
				propertyPath,
				isShallow()
		);
	}
}
