/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.internal.resolver;

import java.util.Collections;
import java.util.List;

import org.hibernate.AssertionFailure;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.metamodel.internal.binder.BinderRootContext;
import org.hibernate.metamodel.internal.binder.ForeignKeyHelper;
import org.hibernate.metamodel.internal.binder.RelationalValueBindingHelper;
import org.hibernate.metamodel.source.spi.AssociationSource;
import org.hibernate.metamodel.source.spi.MappedByAssociationSource;
import org.hibernate.metamodel.source.spi.PluralAttributeElementSourceManyToMany;
import org.hibernate.metamodel.source.spi.PluralAttributeSource;
import org.hibernate.metamodel.source.spi.ToOneAttributeSource;
import org.hibernate.metamodel.spi.LocalBindingContext;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.AttributeBindingContainer;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.ManyToOneAttributeBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeElementBindingManyToMany;
import org.hibernate.metamodel.spi.binding.RelationalValueBinding;
import org.hibernate.metamodel.spi.binding.SecondaryTable;
import org.hibernate.metamodel.spi.binding.SingularAssociationAttributeBinding;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.ForeignKey;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.type.ForeignKeyDirection;

/**
 + * @author Gail Badner
 + */
public class MappedByAssociationRelationalBindingResolverImpl implements AssociationRelationalBindingResolver {
	private final BinderRootContext helperContext;

	public MappedByAssociationRelationalBindingResolverImpl(BinderRootContext helperContext) {
		this.helperContext = helperContext;
	}

	@Override
	public SingularAttributeBinding resolveOneToOneReferencedAttributeBinding(
			ToOneAttributeSource attributeSource,
			EntityBinding referencedEntityBinding) {
		return (SingularAttributeBinding) referencedEntityBinding.locateAttributeBindingByPath(
				getMappedByAssociationSource( attributeSource ).getMappedBy(),
				true
		);
	}

	@Override
	public List<RelationalValueBinding> resolveOneToOneRelationalValueBindings(
			final ToOneAttributeSource attributeSource,
			AttributeBindingContainer attributeBindingContainer,
			SingularAttributeBinding referencedAttributeBinding) {
		return Collections.emptyList();
	}

	@Override
	public ForeignKey resolveOneToOneForeignKey(
			ToOneAttributeSource attributeSource,
			TableSpecification sourceTable,
			List<Column> sourceColumns,
			EntityBinding referencedEntityBinding) {
		// TODO: get rid of this duplication!!!
		if ( attributeSource.getForeignKeyDirection() == ForeignKeyDirection.TO_PARENT ) {
			throw new AssertionFailure( "Cannot create a foreign key for one-to-one with foreign key direction going to the parent." );
		}
		final List<Column> targetColumns = foreignKeyHelper().determineForeignKeyTargetColumns(
				referencedEntityBinding,
				attributeSource
		);
		final TableSpecification targetTable = foreignKeyHelper().determineForeignKeyTargetTable(
				referencedEntityBinding,
				attributeSource
		);
		return foreignKeyHelper().locateOrCreateForeignKey(
				attributeSource.getExplicitForeignKeyName(),
				sourceTable,
				sourceColumns,
				targetTable,
				targetColumns,
				attributeSource.isCascadeDeleteEnabled(),
				attributeSource.createForeignKeyConstraint()
		);
	}

	@Override
	public SingularAttributeBinding resolveManyToOneReferencedAttributeBinding(
			AttributeBindingContainer attributeBindingContainer,
			ToOneAttributeSource attributeSource,
			EntityBinding referencedEntityBinding) {
		// This is a mappedBy many-to-one. This should only happen when the owning many-to-one uses a join table.
		// TODO: confirm the above is true.
		final SecondaryTable ownerSecondaryTable = getOwnerSecondaryTable(
				getMappedByAssociationSource( attributeSource ),
				referencedEntityBinding
		);
		return referencedEntityBinding.locateAttributeBinding(
				ownerSecondaryTable.getForeignKeyReference().getTargetTable(),
				ownerSecondaryTable.getForeignKeyReference().getTargetColumns(),
				true
		);
	}

	@Override
	public List<RelationalValueBinding> resolveManyToOneRelationalValueBindings(
			ToOneAttributeSource attributeSource,
			AttributeBindingContainer attributeBindingContainer,
			SingularAttributeBinding referencedAttributeBinding,
			EntityBinding referencedEntityBinding) {
		// A many-to-one can only have mappedBy specified if there is a join table.
		// TODO: confirm this is true.
		// The relational value bindings for the attribute being processed
		// will contain the columns that make up the FK join tables on the
		// owner's secondary table.
		final SecondaryTable ownerSecondaryTable = getOwnerSecondaryTable(
				getMappedByAssociationSource( attributeSource ),
				referencedEntityBinding
		);
		return relationalValueBindingHelper().bindInverseRelationalValueBindings(
				ownerSecondaryTable.getForeignKeyReference().getSourceTable(),
				ownerSecondaryTable.getForeignKeyReference().getSourceColumns()
		);
	}

	@Override
	public ForeignKey resolveManyToOneForeignKey(
			ToOneAttributeSource attributeSource,
			AttributeBindingContainer attributeBindingContainer,
			List<RelationalValueBinding> relationalValueBindings,
			EntityBinding referencedEntityBinding) {
		// A many-to-one can only have mappedBy specified if there is a join table.
		// TODO: confirm this is true.
		final SecondaryTable ownerSecondaryTable = getOwnerSecondaryTable(
				getMappedByAssociationSource( attributeSource ),
				referencedEntityBinding
		);
		return ownerSecondaryTable.getForeignKeyReference();
	}

	@Override
	public List<RelationalValueBinding> resolveManyToManyElementRelationalValueBindings(
			final EntityBinding entityBinding,
			final PluralAttributeElementSourceManyToMany elementSource,
			final TableSpecification collectionTable,
			final EntityBinding referencedEntityBinding) {
		{
			final AttributeBinding ownerAttributeBinding = getOwnerAttributeBinding(
					getMappedByAssociationSource( elementSource )
			);
			final List<RelationalValueBinding> relationalValueBindings;
			if ( ownerAttributeBinding.getAttribute().isSingular() ) {
				// the owner is a many-to-one on a join table; the target will be the FK target
				// for the secondary table.
				final SecondaryTable ownerSecondaryTable =
						referencedEntityBinding.getSecondaryTables().get( collectionTable.getLogicalName() );
				relationalValueBindings = relationalValueBindingHelper().bindInverseRelationalValueBindings(
						collectionTable,
						ownerSecondaryTable.getForeignKeyReference().getSourceColumns()
				);
			}
			else {
				final PluralAttributeBinding ownerPluralAttributeBinding = (PluralAttributeBinding) ownerAttributeBinding;
				relationalValueBindings = relationalValueBindingHelper().bindInverseRelationalValueBindings(
						collectionTable,
						ownerPluralAttributeBinding.getPluralAttributeKeyBinding().getValues()
				);
			}
			return relationalValueBindings;
		}
	}

	@Override
	public ForeignKey resolveManyToManyElementForeignKey(
		final EntityBinding entityBinding,
		final PluralAttributeElementSourceManyToMany elementSource,
		final TableSpecification collectionTable,
		final List<RelationalValueBinding> relationalValueBindings,
		final EntityBinding referencedEntityBinding) {
		final AttributeBinding ownerAttributeBinding = getOwnerAttributeBinding(
				getMappedByAssociationSource( elementSource )
		);
		if ( ownerAttributeBinding.getAttribute().isSingular() ) {
			// the owner is a many-to-one on a join table; the target will be the FK target
			// for the secondary table.
			final SecondaryTable ownerSecondaryTable =
					referencedEntityBinding.getSecondaryTables().get( collectionTable.getLogicalName() );
			return ownerSecondaryTable.getForeignKeyReference();
		}
		else {
			final PluralAttributeBinding ownerPluralAttributeBinding = (PluralAttributeBinding) ownerAttributeBinding;
			return ownerPluralAttributeBinding.getPluralAttributeKeyBinding().getForeignKey();
		}
	}

	@Override
	public TableSpecification resolveManyToManyCollectionTable(
			PluralAttributeSource pluralAttributeSource,
			String attributePath,
			EntityBinding entityBinding,
			EntityBinding referencedEntityBinding) {
		final AttributeBinding ownerAttributeBinding = getOwnerAttributeBinding(
				getMappedByAssociationSource( (AssociationSource) pluralAttributeSource.getElementSource() )
		);
		return ownerAttributeBinding.getAttribute().isSingular() ?
				( (SingularAssociationAttributeBinding) ownerAttributeBinding ).getTable() :
				( (PluralAttributeBinding) ownerAttributeBinding ).getPluralAttributeKeyBinding().getCollectionTable();
	}

	@Override
	public List<RelationalValueBinding> resolvePluralAttributeKeyRelationalValueBindings(
			PluralAttributeSource attributeSource,
			EntityBinding entityBinding,
			TableSpecification collectionTable,
			EntityBinding referencedEntityBinding) {
		final AttributeBinding ownerAttributeBinding = getOwnerAttributeBinding(
				getMappedByAssociationSource( (AssociationSource) attributeSource.getElementSource() )
		);
		if ( ownerAttributeBinding.getAttribute().isSingular() ) {
			return ( (ManyToOneAttributeBinding) ownerAttributeBinding ).getRelationalValueBindings();
		}
		else {
			final PluralAttributeBinding pluralOwnerAttributeBinding = (PluralAttributeBinding) ownerAttributeBinding;
			final PluralAttributeElementBindingManyToMany ownerElementBinding =
					(PluralAttributeElementBindingManyToMany) pluralOwnerAttributeBinding.getPluralAttributeElementBinding();
			return ownerElementBinding.getRelationalValueContainer().relationalValueBindings();
		}
	}

	@Override
	public ForeignKey resolvePluralAttributeKeyForeignKey(
			PluralAttributeSource attributeSource,
			EntityBinding entityBinding,
			TableSpecification collectionTable,
			List<RelationalValueBinding> sourceRelationalValueBindings,
			EntityBinding referencedEntityBinding) {
		final AttributeBinding ownerAttributeBinding = getOwnerAttributeBinding(
				getMappedByAssociationSource( (AssociationSource) attributeSource.getElementSource() )
		);
		final ForeignKey foreignKey;
		if ( ownerAttributeBinding.getAttribute().isSingular() ) {
			foreignKey = ( (ManyToOneAttributeBinding) ownerAttributeBinding ).getForeignKey();
		}
		else {
			final PluralAttributeBinding pluralOwnerAttributeBinding = (PluralAttributeBinding) ownerAttributeBinding;
			final PluralAttributeElementBindingManyToMany ownerElementBinding =
					(PluralAttributeElementBindingManyToMany) pluralOwnerAttributeBinding.getPluralAttributeElementBinding();
			foreignKey = ownerElementBinding.getForeignKey();
		}
		if ( attributeSource.getKeySource().isCascadeDeleteEnabled() ) {
			foreignKey.setDeleteRule( ForeignKey.ReferentialAction.CASCADE );
		}
		return foreignKey;

	}

	@Override
	public SingularAttributeBinding resolvePluralAttributeKeyReferencedBinding(
			AttributeBindingContainer attributeBindingContainer,
			PluralAttributeSource attributeSource) {
		final AttributeBinding ownerAttributeBinding = getOwnerAttributeBinding(
				getMappedByAssociationSource( (AssociationSource) attributeSource.getElementSource() )
		);
		final SingularAttributeBinding referencedAttributeBinding;
		if ( ownerAttributeBinding.getAttribute().isSingular() ) {
			referencedAttributeBinding =
					( (SingularAssociationAttributeBinding) ownerAttributeBinding ).getReferencedAttributeBinding();
		}
		else {
			final PluralAttributeBinding ownerPluralAttributeBinding = (PluralAttributeBinding) ownerAttributeBinding;
			final PluralAttributeElementBindingManyToMany ownerElementBinding =
					(PluralAttributeElementBindingManyToMany) ownerPluralAttributeBinding
							.getPluralAttributeElementBinding();
			referencedAttributeBinding = attributeBindingContainer.seekEntityBinding().locateAttributeBinding(
					ownerElementBinding.getForeignKey().getTargetTable(),
					ownerElementBinding.getForeignKey().getTargetColumns(),
					true
			);
			if ( referencedAttributeBinding == null ) {
				throw new NotYetImplementedException( "Referenced columns not used by an attribute binding is not supported yet." );
			}
		}
		return referencedAttributeBinding;
	}

	private LocalBindingContext bindingContext() {
		return helperContext.getLocalBindingContextSelector().getCurrentBinderLocalBindingContext();
	}

	private MappedByAssociationSource getMappedByAssociationSource(AssociationSource associationSource) {
		if ( !associationSource.isMappedBy() || !MappedByAssociationSource.class.isInstance( associationSource ) ) {
			throw new AssertionFailure( "Expected a MappedByAssociationSource." );
		}
		return (MappedByAssociationSource) associationSource;
	}

	private AttributeBinding getOwnerAttributeBinding(MappedByAssociationSource associationSource) {
		final EntityBinding referencedEntityBinding =   bindingContext().getMetadataCollector().getEntityBinding(
				associationSource.getReferencedEntityName()
		);
		final AttributeBinding ownerAttributeBinding = referencedEntityBinding.locateAttributeBindingByPath(
				associationSource.getMappedBy(),
				true
		);
		if ( ownerAttributeBinding == null ) {
			throw bindingContext().makeMappingException(
					String.format(
							"Attribute not found: [%s.%s]",
							referencedEntityBinding.getEntityName(),
							associationSource.getMappedBy()
					)
			);
		}
		return ownerAttributeBinding;
	}

	private SecondaryTable getOwnerSecondaryTable(
			MappedByAssociationSource attributeSource,
			EntityBinding referencedEntityBinding) {
		SingularAssociationAttributeBinding ownerAttributeBinding =
				(SingularAssociationAttributeBinding) referencedEntityBinding.locateAttributeBinding(
						attributeSource.getMappedBy()
				);
		TableSpecification table = ownerAttributeBinding.getTable();
		if ( referencedEntityBinding.getPrimaryTable().equals( table ) ) {
			throw new AssertionFailure( "many-to-one has mappedby specified but it does not use a join table." );
		}
		return referencedEntityBinding.getSecondaryTables().get( table.getLogicalName() );
	}

	private ForeignKeyHelper foreignKeyHelper() {
		return helperContext.foreignKeyHelper();
	}

	private RelationalValueBindingHelper relationalValueBindingHelper() {
		return helperContext.relationalValueBindingHelper();
	}
}
