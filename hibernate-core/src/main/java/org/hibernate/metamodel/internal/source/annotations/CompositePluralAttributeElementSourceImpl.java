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
import java.util.HashMap;
import java.util.List;

import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.metamodel.internal.source.annotations.attribute.AssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.AttributeOverride;
import org.hibernate.metamodel.internal.source.annotations.attribute.BasicAttribute;
import org.hibernate.metamodel.internal.source.annotations.entity.EmbeddableClass;
import org.hibernate.metamodel.internal.source.annotations.entity.RootEntityClass;
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
	private final RootEntityClass rootEntityClass;
	private final Iterable<CascadeStyle> cascadeStyles;
	
	private List<AttributeSource> attributeSources
			= new ArrayList<AttributeSource>();
	
	public CompositePluralAttributeElementSourceImpl(
			AssociationAttribute associationAttribute,
			RootEntityClass rootEntityClass,
			Iterable<CascadeStyle> cascadeStyles ) {
		this.associationAttribute = associationAttribute;
		this.rootEntityClass = rootEntityClass;
		this.cascadeStyles = cascadeStyles;
		
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
		return cascadeStyles;
	}

	@Override
	public Iterable<? extends MetaAttributeSource> getMetaAttributeSources() {
		// HBM only
		return null;
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getExplicitTuplizerClassName() {
		// TODO Auto-generated method stub
		return null;
	}
	
	private void buildAttributeSources() {
		// TODO: Duplicates code in ComponentAttributeSourceImpl.
		EmbeddableClass embeddableClass = rootEntityClass.getEmbeddedClasses()
				.get( associationAttribute.getName() );
		for ( BasicAttribute attribute : embeddableClass.getSimpleAttributes() ) {
			AttributeOverride attributeOverride = null;
			String tmp = getPath() + PATH_SEPARATOR + attribute.getName();
//			if ( attributeOverrides.containsKey( tmp ) ) {
//				attributeOverride = attributeOverrides.get( tmp );
//			}
			attribute.setNaturalIdMutability( embeddableClass.getNaturalIdMutability() );
			attributeSources.add( new SingularAttributeSourceImpl( attribute, attributeOverride ) );
		}
		for ( EmbeddableClass embeddable : embeddableClass.getEmbeddedClasses().values() ) {
			embeddable.setNaturalIdMutability( embeddableClass.getNaturalIdMutability() );
			attributeSources.add(
					new ComponentAttributeSourceImpl(
							embeddable,
							getPath(),
//							createAggregatedOverrideMap()
							new HashMap<String, AttributeOverride>()
					)
			);
		}
		for ( AssociationAttribute associationAttribute : embeddableClass.getAssociationAttributes() ) {
			associationAttribute.setNaturalIdMutability( embeddableClass.getNaturalIdMutability() );
			attributeSources.add( new ToOneAttributeSourceImpl( associationAttribute ) );
		}
	}

}
