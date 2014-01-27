/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.persistence.AccessType;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.internal.source.annotations.attribute.AssociationOverride;
import org.hibernate.metamodel.internal.source.annotations.attribute.AttributeOverride;
import org.hibernate.metamodel.internal.source.annotations.entity.EmbeddableClass;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.source.AttributeSource;
import org.hibernate.metamodel.spi.source.ComponentAttributeSource;
import org.hibernate.metamodel.spi.source.HibernateTypeSource;
import org.hibernate.metamodel.spi.source.LocalBindingContext;
import org.hibernate.metamodel.spi.source.MetaAttributeSource;
import org.hibernate.metamodel.spi.source.RelationalValueSource;

/**
 * Annotation backed implementation of {@code ComponentAttributeSource}.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 * @author Brett Meyer
 */
public class ComponentAttributeSourceImpl implements ComponentAttributeSource, AnnotationAttributeSource {
	private final EmbeddableClass embeddableClass;
	private final ValueHolder<Class<?>> classReference;
	private final String path;
	private final AccessType classAccessType;
	private Map<String, AttributeOverride> attributeOverrideMap;
	private Map<String, AssociationOverride> associationOverrideMap;
	public ComponentAttributeSourceImpl(
			final EmbeddableClass embeddableClass,
			final String parentPath,
			final AccessType classAccessType) {
		this.embeddableClass = embeddableClass;
		this.classReference = new ValueHolder<Class<?>>( embeddableClass.getConfiguredClass() );
		this.path = StringHelper.isEmpty( parentPath ) ? embeddableClass.getEmbeddedAttributeName() : parentPath + "." + embeddableClass.getEmbeddedAttributeName();
		this.classAccessType = classAccessType;
	}

	@Override
	public void applyAssociationOverride(Map<String, AssociationOverride> associationOverrideMap) {
		this.associationOverrideMap = associationOverrideMap;
	}

	@Override
	public void applyAttributeOverride(Map<String, AttributeOverride> attributeOverrideMap) {
		this.attributeOverrideMap = attributeOverrideMap;
	}

	@Override
	public boolean isVirtualAttribute() {
		return false;
	}

	@Override
	public Nature getNature() {
		return Nature.COMPOSITE;
	}

	@Override
	public boolean isSingular() {
		return true;
	}

	@Override
	public String getClassName() {
		return embeddableClass.getConfiguredClass().getName();
	}

	@Override
	public ValueHolder<Class<?>> getClassReference() {
		return classReference;
	}

	@Override
	public String getName() {
		return embeddableClass.getEmbeddedAttributeName();
	}

	@Override
	public String getExplicitTuplizerClassName() {
		return StringHelper.isEmpty( embeddableClass.getCustomTuplizerClass() ) ? embeddableClass.getCustomTuplizer() : embeddableClass
				.getCustomTuplizerClass();
	}

	@Override
	public String getPropertyAccessorName() {
		return classAccessType.toString().toLowerCase();
	}

	@Override
	public LocalBindingContext getLocalBindingContext() {
		return embeddableClass.getLocalBindingContext();
	}

//	private final ValueHolder<List<AttributeSource>> attributeSourcesValue = new ValueHolder<List<AttributeSource>>(
//			new ValueHolder.DeferredInitializer<List<AttributeSource>>() {
//				@Override
//				public List<AttributeSource> initialize() {
//					List<AttributeSource> attributeList = new ArrayList<AttributeSource>();
//					for ( BasicAttribute attribute : embeddableClass.getSimpleAttributes().values() ) {
//						attribute.setNaturalIdMutability( embeddableClass.getNaturalIdMutability() );
//						attributeList.add( new SingularAttributeSourceImpl( attribute ) );
//					}
//					for ( EmbeddableClass embeddable : embeddableClass.getEmbeddedClasses().values() ) {
//						embeddable.setNaturalIdMutability( embeddableClass.getNaturalIdMutability() );
//						attributeList.add(
//								new ComponentAttributeSourceImpl(
//										embeddable,
//										getPath(),
//										classAccessType
//								)
//						);
//					}
//					for ( AssociationAttribute associationAttribute : embeddableClass.getAssociationAttributes().values() ) {
//						associationAttribute.setNaturalIdMutability( embeddableClass.getNaturalIdMutability() );
//					}
//					SourceHelper.resolveAssociationAttributes( embeddableClass, attributeList );
//					return Collections.unmodifiableList( attributeList );
//				}
//			}
//	);

	@Override
	public List<AttributeSource> attributeSources() {
//		return attributeSourcesValue.getValue();
		return SourceHelper.resolveAttributes( embeddableClass, getPath(), attributeOverrideMap, associationOverrideMap ).getValue();
	}

	@Override
	public String getPath() {
		return path;
	}

	@Override
	public String getParentReferenceAttributeName() {
		return embeddableClass.getParentReferencingAttributeName();
	}

	@Override
	public Iterable<MetaAttributeSource> getMetaAttributeSources() {
		// not relevant for annotations
		return Collections.emptySet();
	}

	@Override
	public String getContainingTableName() {
		// none, it is defined on the sub-attributes
		return null;
	}

	@Override
	public List<RelationalValueSource> relationalValueSources() {
		// none, they are defined on the sub-attributes
		return null;
	}

	@Override
	public HibernateTypeSource getTypeInformation() {
		// probably need to check for @Target in EmbeddableClass (HF)
		return null;
	}

	@Override
	public PropertyGeneration getGeneration() {
		return null;
	}

	@Override
	public boolean isLazy() {
		return false;
	}

	@Override
	public SingularAttributeBinding.NaturalIdMutability getNaturalIdMutability() {
		return  embeddableClass.getNaturalIdMutability();
	}

	@Override
	public boolean isIncludedInOptimisticLocking() {
		return true;
	}

	@Override
	public boolean areValuesIncludedInInsertByDefault() {
		return true;
	}

	@Override
	public boolean areValuesIncludedInUpdateByDefault() {
		return true;
	}

	@Override
	public boolean areValuesNullableByDefault() {
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "ComponentAttributeSourceImpl" );
		sb.append( "{embeddableClass=" ).append( embeddableClass.getConfiguredClass().getSimpleName() );
		sb.append( '}' );
		return sb.toString();
	}

}
