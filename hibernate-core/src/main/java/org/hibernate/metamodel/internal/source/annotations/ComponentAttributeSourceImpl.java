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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.internal.source.annotations.attribute.AssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.AttributeOverride;
import org.hibernate.metamodel.internal.source.annotations.attribute.BasicAttribute;
import org.hibernate.metamodel.internal.source.annotations.entity.EmbeddableClass;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.source.AttributeSource;
import org.hibernate.metamodel.spi.source.ComponentAttributeSource;
import org.hibernate.metamodel.spi.source.ExplicitHibernateTypeSource;
import org.hibernate.metamodel.spi.source.LocalBindingContext;
import org.hibernate.metamodel.spi.source.MetaAttributeSource;
import org.hibernate.metamodel.spi.source.RelationalValueSource;

/**
 * Annotation backed implementation of {@code ComponentAttributeSource}.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public class ComponentAttributeSourceImpl implements ComponentAttributeSource {
	private static final String PATH_SEPARATOR = ".";
	private final EmbeddableClass embeddableClass;
	private final ValueHolder<Class<?>> classReference;
	private final Map<String, AttributeOverride> attributeOverrides;
	private final String path;

	public ComponentAttributeSourceImpl(EmbeddableClass embeddableClass, String parentPath, Map<String, AttributeOverride> attributeOverrides) {
		this.embeddableClass = embeddableClass;
		this.classReference = new ValueHolder<Class<?>>( embeddableClass.getConfiguredClass() );
		this.attributeOverrides = attributeOverrides;
		this.path = StringHelper.isEmpty( parentPath ) ? embeddableClass.getEmbeddedAttributeName() : parentPath + "." + embeddableClass.getEmbeddedAttributeName();
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
		return embeddableClass.getCustomTuplizer();
	}

	@Override
	public String getPropertyAccessorName() {
		return embeddableClass.getClassAccessType().toString().toLowerCase();
	}

	@Override
	public LocalBindingContext getLocalBindingContext() {
		return embeddableClass.getLocalBindingContext();
	}

	private final ValueHolder<List<AttributeSource>> attributeSourcesValue = new ValueHolder<List<AttributeSource>>(
			new ValueHolder.DeferredInitializer<List<AttributeSource>>() {
				@Override
				public List<AttributeSource> initialize() {
					List<AttributeSource> attributeList = new ArrayList<AttributeSource>();
					for ( BasicAttribute attribute : embeddableClass.getSimpleAttributes() ) {
						AttributeOverride attributeOverride = null;
						String tmp = getPath() + PATH_SEPARATOR + attribute.getName();
						if ( attributeOverrides.containsKey( tmp ) ) {
							attributeOverride = attributeOverrides.get( tmp );
						}
						attribute.setNaturalIdMutability( embeddableClass.getNaturalIdMutability() );
						attributeList.add( new SingularAttributeSourceImpl( attribute, attributeOverride ) );
					}
					for ( EmbeddableClass embeddable : embeddableClass.getEmbeddedClasses().values() ) {
						embeddable.setNaturalIdMutability( embeddableClass.getNaturalIdMutability() );
						attributeList.add(
								new ComponentAttributeSourceImpl(
										embeddable,
										getPath(),
										createAggregatedOverrideMap()
								)
						);
					}
					for ( AssociationAttribute associationAttribute : embeddableClass.getAssociationAttributes() ) {
						associationAttribute.setNaturalIdMutability( embeddableClass.getNaturalIdMutability() );
						attributeList.add( new ToOneAttributeSourceImpl( associationAttribute ) );
					}
					return Collections.unmodifiableList( attributeList );
				}
			}
	);

	@Override
	public List<AttributeSource> attributeSources() {
		return attributeSourcesValue.getValue();
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
	public List<RelationalValueSource> relationalValueSources() {
		// none, they are defined on the simple sub-attributes
		return null;
	}

	@Override
	public ExplicitHibernateTypeSource getTypeInformation() {
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

	private Map<String, AttributeOverride> createAggregatedOverrideMap() {
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
