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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.metamodel.internal.source.annotations.attribute.AssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.AssociationOverride;
import org.hibernate.metamodel.internal.source.annotations.attribute.AttributeOverride;
import org.hibernate.metamodel.internal.source.annotations.attribute.PluralAssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.entity.ConfiguredClass;
import org.hibernate.metamodel.internal.source.annotations.entity.EmbeddableClass;
import org.hibernate.metamodel.spi.binding.CascadeType;
import org.hibernate.metamodel.spi.source.AttributeSource;
import org.hibernate.metamodel.spi.source.CompositePluralAttributeElementSource;
import org.hibernate.metamodel.spi.source.LocalBindingContext;
import org.hibernate.metamodel.spi.source.MetaAttributeSource;
import org.hibernate.metamodel.spi.source.PluralAttributeSource;

/**
 * @author Brett Meyer
 */
public class CompositePluralAttributeElementSourceImpl implements CompositePluralAttributeElementSource,AnnotationAttributeSource {
	private final AssociationAttribute associationAttribute;
	private final ConfiguredClass entityClass;
	
//	private final List<AttributeSource> attributeSources
//			= new ArrayList<AttributeSource>();
//
	private final String parentReferenceAttributeName;
	private final EmbeddableClass embeddableClass;

	private final String overridePath;

	private Map<String, AttributeOverride> attributeOverrideMap;
	private Map<String, AssociationOverride> associationOverrideMap;
	
	public CompositePluralAttributeElementSourceImpl(
			final PluralAssociationAttribute associationAttribute,
			final ConfiguredClass rootEntityClass,
			final String parentPath) {
		this.associationAttribute = associationAttribute;
		this.entityClass = rootEntityClass;
		this.embeddableClass = entityClass
				.getCollectionEmbeddedClasses()
				.get( associationAttribute.getName() );

		this.parentReferenceAttributeName = embeddableClass.getParentReferencingAttributeName();
		if ( associationAttribute.getPluralAttributeNature() == PluralAttributeSource.Nature.MAP ) {
			this.overridePath = parentPath + ".value";
		}
		else {
			this.overridePath = parentPath + ".element";

		}
	}

	@Override
	public void applyAssociationOverride(Map<String, AssociationOverride> associationOverrideMap) {
		this.associationOverrideMap = associationOverrideMap;
	}

	@Override
	public void applyAttributeOverride(Map<String, AttributeOverride> attributeOverrideMap) {
		this.attributeOverrideMap  = attributeOverrideMap;
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
		return SourceHelper.resolveAttributes( embeddableClass, overridePath, attributeOverrideMap, associationOverrideMap ).getValue();
	}

	@Override
	public LocalBindingContext getLocalBindingContext() {
		return associationAttribute.getContext();
	}

	@Override
	public Set<CascadeStyle> getCascadeStyles() {
		Set<CascadeStyle> cascadeStyles = new HashSet<CascadeStyle>();
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
	
//	private void buildAttributeSources() {
//		// TODO: Duplicates code in ComponentAttributeSourceImpl.
//		for ( BasicAttribute attribute : embeddableClass.getSimpleAttributes().values() ) {
//			attribute.setNaturalIdMutability( embeddableClass.getNaturalIdMutability() );
//			attributeSources.add( new SingularAttributeSourceImpl( attribute ) );
//		}
//		for ( EmbeddableClass embeddable : embeddableClass.getEmbeddedClasses().values() ) {
//			embeddable.setNaturalIdMutability( embeddableClass.getNaturalIdMutability() );
//			attributeSources.add(
//					new ComponentAttributeSourceImpl(
//							embeddable,
//							getPath(),
//							embeddableClass.getClassAccessType()
//					)
//			);
//		}
//		for ( AssociationAttribute associationAttribute : embeddableClass.getAssociationAttributes().values() ) {
//			associationAttribute.setNaturalIdMutability( embeddableClass.getNaturalIdMutability() );
//		}
//		SourceHelper.resolveAssociationAttributes( embeddableClass, attributeSources );
//	}
}
