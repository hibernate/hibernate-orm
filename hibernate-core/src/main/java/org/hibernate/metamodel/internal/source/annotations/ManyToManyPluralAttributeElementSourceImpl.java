/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.metamodel.internal.source.annotations.attribute.Column;
import org.hibernate.metamodel.internal.source.annotations.attribute.PluralAssociationAttribute;
import org.hibernate.metamodel.spi.binding.CascadeType;
import org.hibernate.metamodel.spi.source.ManyToManyPluralAttributeElementSource;
import org.hibernate.metamodel.spi.source.PluralAttributeElementNature;
import org.hibernate.metamodel.spi.source.RelationalValueSource;

/**
 * @author Hardy Ferentschik
 */
public class ManyToManyPluralAttributeElementSourceImpl implements ManyToManyPluralAttributeElementSource {
	private final PluralAssociationAttribute associationAttribute;

	public ManyToManyPluralAttributeElementSourceImpl(PluralAssociationAttribute associationAttribute) {
		this.associationAttribute = associationAttribute;
	}

	@Override
	public String getReferencedEntityName() {
		return associationAttribute.getReferencedEntityType();
	}

	@Override
	public String getReferencedEntityAttributeName() {
		// JPA does not have the concept of property refs. Instead column names via @JoinColumn are used
		return null;
	}

	@Override
	public Collection<String> getReferencedColumnNames() {
		HashSet<String> referencedColumnNames = new HashSet<String>();
		for ( Column column : associationAttribute.getColumnValues() ) {
			if ( column.getReferencedColumnName() != null ) {
				referencedColumnNames.add( column.getReferencedColumnName() );
			}
		}
		return referencedColumnNames;
	}

	@Override
	public List<RelationalValueSource> getValueSources() {
		List<RelationalValueSource> valueSources = new ArrayList<RelationalValueSource>();
		// todo
		return valueSources;
	}

	@Override
	public Iterable<CascadeStyle> getCascadeStyles() {
		List<CascadeStyle> cascadeStyles = new ArrayList<CascadeStyle>();
		for ( javax.persistence.CascadeType cascadeType : associationAttribute.getCascadeTypes() ) {
			cascadeStyles.add( CascadeType.getCascadeType( cascadeType ).toCascadeStyle() );
		}
		return cascadeStyles;
	}

	@Override
	public boolean isNotFoundAnException() {
		return !associationAttribute.isIgnoreNotFound();
	}

	@Override
	public String getExplicitForeignKeyName() {
		return associationAttribute.getInverseForeignKeyName();
	}

	@Override
	public boolean isUnique() {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public String getOrderBy() {
		return associationAttribute.getOrderBy();
	}

	@Override
	public String getWhere() {
		return associationAttribute.getWhereClause();
	}

	@Override
	public boolean fetchImmediately() {
		return associationAttribute.isLazy();
	}

	@Override
	public PluralAttributeElementNature getNature() {
		return PluralAttributeElementNature.MANY_TO_MANY;
	}
}


