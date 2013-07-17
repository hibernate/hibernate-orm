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
package org.hibernate.metamodel.internal.source.annotations;

import java.util.List;

import org.hibernate.metamodel.internal.Binder;
import org.hibernate.metamodel.internal.source.annotations.attribute.MappedAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.SingularAssociationAttribute;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.source.AttributeSourceResolutionContext;
import org.hibernate.metamodel.spi.source.MappedByAssociationSource;
import org.hibernate.metamodel.spi.source.RelationalValueSource;
import org.hibernate.metamodel.spi.source.ToOneAttributeSource;

/**
 * @author Gail Badner
 */
public class ToOneMappedByAttributeSourceImpl extends AbstractToOneAttributeSourceImpl implements MappedByAssociationSource {
	private ToOneAttributeSource owner;

	public ToOneMappedByAttributeSourceImpl(SingularAssociationAttribute associationAttribute, String relativePath) {
		super( associationAttribute, relativePath);
		if ( associationAttribute.getMappedBy() == null ) {
			throw new IllegalArgumentException( "associationAttribute.getMappedBy() must be non-null" );
		}
	}

	@Override
	public void resolveToOneAttributeSource(AttributeSourceResolutionContext context) {
		if ( getNature() != null && owner != null) {
			return;
		}
		if ( owner == null ) {
			owner = (ToOneAttributeSource) context.resolveAttributeSource(
					associationAttribute().getReferencedEntityType(),
					associationAttribute().getMappedBy()
			);
			owner.addMappedByAssociationSource( this );
		}
		if ( getNature() == null ) {
			final Nature nature;
			if ( MappedAttribute.Nature.MANY_TO_ONE.equals( associationAttribute().getNature() ) ) {
				nature = Nature.MANY_TO_ONE;
			}
			else if ( MappedAttribute.Nature.ONE_TO_ONE.equals( associationAttribute().getNature() ) ) {
				if ( owner.getContainingTableName() != null ) {
					nature = Nature.MANY_TO_ONE;
				}
				else  {
					nature = Nature.ONE_TO_ONE;
				}
			}
			else {
				throw new AssertionError(String.format(
						"Wrong attribute nature[%s] for toOne attribute: %s",
						associationAttribute().getNature(), associationAttribute().getRole()
				));
			}
			setNature( nature );
		}
	}

	@Override
	public boolean isMappedBy() {
		return true;
	}

	@Override
	public String getMappedBy() {
		return associationAttribute().getMappedBy();
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
	public JoinColumnResolutionDelegate getForeignKeyTargetColumnResolutionDelegate() {
		throw new UnsupportedOperationException( "Not supported for a \"mappedBy\" association." );
	}

	@Override
	public List<Binder.DefaultNamingStrategy> getDefaultNamingStrategies(String entityName, String tableName, AttributeBinding referencedAttributeBinding) {
		throw new UnsupportedOperationException( "Not supported for a \"mappedBy\" association." );
	}
}
