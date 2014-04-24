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
package org.hibernate.metamodel.source.internal.annotations;

import java.util.List;

import org.hibernate.metamodel.internal.binder.Binder;
import org.hibernate.metamodel.source.internal.annotations.attribute.AbstractPersistentAttribute;
import org.hibernate.metamodel.source.internal.annotations.attribute.AssociationOverride;
import org.hibernate.metamodel.source.internal.annotations.attribute.OverrideAndConverterCollector;
import org.hibernate.metamodel.source.internal.annotations.attribute.SingularAssociationAttribute;
import org.hibernate.metamodel.source.spi.AttributeSourceResolutionContext;
import org.hibernate.metamodel.source.spi.MappedByAssociationSource;
import org.hibernate.metamodel.source.spi.RelationalValueSource;
import org.hibernate.metamodel.source.spi.ToOneAttributeSource;
import org.hibernate.metamodel.spi.SingularAttributeNature;
import org.hibernate.metamodel.spi.binding.AttributeBinding;

/**
 * @author Gail Badner
 */
public class ToOneMappedByAttributeSourceImpl
		extends AbstractToOneAttributeSourceImpl
		implements MappedByAssociationSource {
	private ToOneAttributeSource owner;

	public ToOneMappedByAttributeSourceImpl(
			SingularAssociationAttribute associationAttribute,
			OverrideAndConverterCollector overrideAndConverterCollector) {
		super( associationAttribute);
		if ( associationAttribute.getMappedByAttributeName() == null ) {
			throw new IllegalArgumentException( "associationAttribute.getMappedByAttributeName() must be non-null" );
		}

		final AssociationOverride override = overrideAndConverterCollector.locateAssociationOverride(
				associationAttribute.getPath()
		);
		if ( override != null ) {
			// todo : do what... exception? warn?
		}
	}

	@Override
	public void resolveToOneAttributeSourceNature(AttributeSourceResolutionContext context) {
		if ( getSingularAttributeNature() != null && owner != null) {
			return;
		}
		if ( owner == null ) {
			owner = (ToOneAttributeSource) context.resolveAttributeSource(
					associationAttribute().getTargetTypeName(),
					associationAttribute().getMappedByAttributeName()
			);
			owner.addMappedByAssociationSource( this );
		}
		if ( getSingularAttributeNature() == null ) {
			final SingularAttributeNature singularAttributeNature;
			if ( AbstractPersistentAttribute.Nature.MANY_TO_ONE.equals( associationAttribute().getNature() ) ) {
				singularAttributeNature = SingularAttributeNature.MANY_TO_ONE;
			}
			else if ( AbstractPersistentAttribute.Nature.ONE_TO_ONE.equals( associationAttribute().getNature() ) ) {
				if ( owner.getContainingTableName() != null ) {
					singularAttributeNature = SingularAttributeNature.MANY_TO_ONE;
				}
				else  {
					singularAttributeNature = SingularAttributeNature.ONE_TO_ONE;
				}
			}
			else {
				throw new AssertionError(String.format(
						"Wrong attribute nature[%s] for toOne attribute: %s",
						associationAttribute().getNature(), associationAttribute().getRole()
				));
			}
			setSingularAttributeNature( singularAttributeNature );
		}
	}

	@Override
	public void resolveToOneAttributeSourceNatureAsPartOfIdentifier() {
		// should this be an error?
	}

	@Override
	public boolean isMappedBy() {
		return true;
	}

	@Override
	public String getMappedBy() {
		return associationAttribute().getMappedByAttributeName();
	}

	@Override
	public List<RelationalValueSource> relationalValueSources() {
		throw new UnsupportedOperationException( "Not supported for a \"mappedBy\" association." );
	}

	@Override
	public String getContainingTableName() {
		throw new UnsupportedOperationException( "Not supported for a \"mappedBy\" association." );
	}

	@Override
	public String getExplicitForeignKeyName() {
		throw new UnsupportedOperationException( "Not supported for a \"mappedBy\" association." );
	}

	@Override
	public boolean createForeignKeyConstraint() {
		throw new UnsupportedOperationException( "Not supported for a \"mappedBy\" association." );
	}

	@Override
	public boolean isCascadeDeleteEnabled() {
		return false;
	}

	@Override
	public JoinColumnResolutionDelegate getForeignKeyTargetColumnResolutionDelegate() {
		throw new UnsupportedOperationException( "Not supported for a \"mappedBy\" association." );
	}

	@Override
	public List<Binder.DefaultNamingStrategy> getDefaultNamingStrategies(String entityName, String tableName, AttributeBinding referencedAttributeBinding) {
		throw new UnsupportedOperationException( "Not supported for a \"mappedBy\" association." );
	}
}
