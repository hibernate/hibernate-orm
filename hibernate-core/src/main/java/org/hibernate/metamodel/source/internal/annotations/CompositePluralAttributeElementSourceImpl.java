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
package org.hibernate.metamodel.source.internal.annotations;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.source.internal.annotations.attribute.PluralAttribute;
import org.hibernate.metamodel.source.internal.annotations.attribute.PluralAttributeElementDetailsEmbedded;
import org.hibernate.metamodel.source.internal.annotations.entity.EmbeddableTypeMetadata;
import org.hibernate.metamodel.source.spi.AttributeSource;
import org.hibernate.metamodel.source.spi.CompositePluralAttributeElementSource;
import org.hibernate.metamodel.source.spi.ToolingHintSource;
import org.hibernate.metamodel.spi.LocalBindingContext;

/**
 * @author Brett Meyer
 */
public class CompositePluralAttributeElementSourceImpl
		implements CompositePluralAttributeElementSource {
	private final PluralAttribute pluralAttribute;
	private final Set<CascadeStyle> unifiedCascadeStyles;
	private final PluralAttributeElementDetailsEmbedded elementDescriptor;

	private final EmbeddableTypeMetadata embeddableTypeMetadata;

	private final List<AttributeSource> attributeSources;

	public CompositePluralAttributeElementSourceImpl(PluralAttributeSourceImpl pluralAttributeSource) {
		this.pluralAttribute = pluralAttributeSource.getPluralAttribute();
		this.unifiedCascadeStyles = pluralAttributeSource.getUnifiedCascadeStyles();

		this.elementDescriptor = (PluralAttributeElementDetailsEmbedded) pluralAttribute.getElementDetails();
		this.embeddableTypeMetadata = elementDescriptor.getEmbeddableTypeMetadata();

		this.attributeSources = SourceHelper.buildAttributeSources(
				embeddableTypeMetadata,
				SourceHelper.PluralAttributesDisallowedAttributeBuilder.INSTANCE
		);
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
		return pluralAttribute.getContext();
	}

	@Override
	public Set<CascadeStyle> getCascadeStyles() {
		return unifiedCascadeStyles;
	}

	@Override
	public Collection<? extends ToolingHintSource> getToolingHintSources() {
		// HBM only
		return Collections.emptyList();
	}

	@Override
	public JavaTypeDescriptor getTypeDescriptor() {
		return elementDescriptor.getJavaType();
	}

	@Override
	public String getParentReferenceAttributeName() {
		return embeddableTypeMetadata.getParentReferencingAttributeName();
	}

	@Override
	public String getExplicitTuplizerClassName() {

		// TODO ?
		return null;
	}
	
//	private void buildAttributeSources() {
//		// TODO: Duplicates code in EmbeddedAttributeSourceImpl.
//		for ( SimpleAttribute attribute : embeddableTypeMetadata.getSimpleAttributes().values() ) {
//			attribute.setNaturalIdMutability( embeddableTypeMetadata.getNaturalIdMutability() );
//			attributeSources.add( new SingularAttributeSourceImpl( attribute ) );
//		}
//		for ( EmbeddableTypeMetadata embeddable : embeddableTypeMetadata.getEmbeddedClasses().values() ) {
//			embeddable.setNaturalIdMutability( embeddableTypeMetadata.getNaturalIdMutability() );
//			attributeSources.add(
//					new EmbeddedAttributeSourceImpl(
//							embeddable,
//							getPath(),
//							embeddableTypeMetadata.getClassLevelAccessType()
//					)
//			);
//		}
//		for ( AssociationAttribute associationAttribute : embeddableTypeMetadata.getAssociationAttributes().values() ) {
//			associationAttribute.setNaturalIdMutability( embeddableTypeMetadata.getNaturalIdMutability() );
//		}
//		SourceHelper.resolveAssociationAttributes( embeddableTypeMetadata, attributeSources );
//	}
}
