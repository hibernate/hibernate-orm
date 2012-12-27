/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.metamodel.internal.source.annotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.metamodel.internal.source.annotations.attribute.AssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.AttributeOverride;
import org.hibernate.metamodel.internal.source.annotations.attribute.BasicAttribute;
import org.hibernate.metamodel.internal.source.annotations.entity.ConfiguredClass;
import org.hibernate.metamodel.internal.source.annotations.entity.EmbeddableClass;
import org.hibernate.metamodel.spi.binding.CascadeType;
import org.hibernate.metamodel.spi.source.AttributeSource;
import org.hibernate.metamodel.spi.source.CompositePluralAttributeElementSource;
import org.hibernate.metamodel.spi.source.LocalBindingContext;
import org.hibernate.metamodel.spi.source.MetaAttributeSource;

/**
 * @author Brett Meyer
 */
public class CompositePluralAttributeElementSourceImpl implements CompositePluralAttributeElementSource {

	private static final String PATH_SEPARATOR = ".";
	
	private final AssociationAttribute associationAttribute;
	private final ConfiguredClass entityClass;
	
	private List<AttributeSource> attributeSources
			= new ArrayList<AttributeSource>();
	
	private String parentReferenceAttributeName;
	
	public CompositePluralAttributeElementSourceImpl(
			AssociationAttribute associationAttribute,
			ConfiguredClass rootEntityClass ) {
		this.associationAttribute = associationAttribute;
		this.entityClass = rootEntityClass;
		
		buildAttributeSources();
	}
	
	@Override
	public Nature getNature() {
		return Nature.AGGREGATE;
	}

	@Override
	public String getPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<AttributeSource> attributeSources() {
		return attributeSources;
	}

	@Override
	public LocalBindingContext getLocalBindingContext() {
		return associationAttribute.getContext();
	}

	@Override
	public Iterable<CascadeStyle> getCascadeStyles() {
		List<CascadeStyle> cascadeStyles = new ArrayList<CascadeStyle>();
		for ( javax.persistence.CascadeType cascadeType : associationAttribute
				.getCascadeTypes() ) {
			cascadeStyles.add( CascadeType.getCascadeType( cascadeType )
					.toCascadeStyle() );
		}
		return cascadeStyles;
	}

	@Override
	public Iterable<? extends MetaAttributeSource> getMetaAttributeSources() {
		// HBM only
		return Collections.emptyList();
	}

	@Override
	public String getClassName() {
		return associationAttribute.getReferencedEntityType();
	}

	@Override
	public ValueHolder<Class<?>> getClassReference() {
		return getLocalBindingContext().makeClassReference( getClassName() );
	}

	@Override
	public String getParentReferenceAttributeName() {
		return parentReferenceAttributeName;
	}

	@Override
	public String getExplicitTuplizerClassName() {
		// TODO ?
		return null;
	}
	
	private void buildAttributeSources() {
		EmbeddableClass embeddableClass = entityClass
				.getCollectionEmbeddedClasses()
				.get( associationAttribute.getName() );
		
		parentReferenceAttributeName = embeddableClass.getParentReferencingAttributeName();
		
		// TODO: Duplicates code in ComponentAttributeSourceImpl.
		for ( BasicAttribute attribute : embeddableClass.getSimpleAttributes() ) {
			AttributeOverride attributeOverride = null;
			String tmp = getPath() + PATH_SEPARATOR + attribute.getName();
			if ( entityClass.getAttributeOverrideMap().containsKey( tmp ) ) {
				attributeOverride = entityClass.getAttributeOverrideMap().get( tmp );
			}
			attribute.setNaturalIdMutability( embeddableClass.getNaturalIdMutability() );
			attributeSources.add( new SingularAttributeSourceImpl( attribute, attributeOverride ) );
		}
		for ( EmbeddableClass embeddable : embeddableClass.getEmbeddedClasses().values() ) {
			embeddable.setNaturalIdMutability( embeddableClass.getNaturalIdMutability() );
			attributeSources.add(
					new ComponentAttributeSourceImpl(
							embeddable,
							getPath(),
							createAggregatedOverrideMap( embeddableClass, entityClass.getAttributeOverrideMap() ),
							embeddable.getClassAccessType()
					)
			);
		}
		for ( AssociationAttribute associationAttribute : embeddableClass.getAssociationAttributes() ) {
			associationAttribute.setNaturalIdMutability( embeddableClass.getNaturalIdMutability() );
		}
		SourceHelper.resolveAssociationAttributes( embeddableClass, attributeSources );
	}

	private Map<String, AttributeOverride> createAggregatedOverrideMap(
			EmbeddableClass embeddableClass,
			Map<String, AttributeOverride> attributeOverrides) {
		// add all overrides passed down to this instance - they override overrides ;-) which are defined further down
		// the embeddable chain
		Map<String, AttributeOverride> aggregatedOverrideMap = new HashMap<String, AttributeOverride>(
				attributeOverrides
		);

		for ( Map.Entry<String, AttributeOverride> entry : embeddableClass.getAttributeOverrideMap().entrySet() ) {
			String fullPath = getPath() + PATH_SEPARATOR + entry.getKey();
			if ( !aggregatedOverrideMap.containsKey( fullPath ) ) {
				aggregatedOverrideMap.put( fullPath, entry.getValue() );
			}
		}
		return aggregatedOverrideMap;
	}

}
