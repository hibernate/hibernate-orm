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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.internal.source.annotations.attribute.AssociationOverride;
import org.hibernate.metamodel.internal.source.annotations.attribute.AttributeOverride;
import org.hibernate.metamodel.internal.source.annotations.attribute.BasicAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.Column;
import org.hibernate.metamodel.internal.source.annotations.attribute.MappedAttribute;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.source.HibernateTypeSource;
import org.hibernate.metamodel.spi.source.MetaAttributeSource;
import org.hibernate.metamodel.spi.source.RelationalValueSource;
import org.hibernate.metamodel.spi.source.SingularAttributeSource;

/**
 * @author Hardy Ferentschik
 */
public class SingularAttributeSourceImpl implements SingularAttributeSource, AnnotationAttributeSource {
	private final MappedAttribute attribute;
	private final HibernateTypeSource type;
	private final String attributePath;

	protected AttributeOverride attributeOverride;

	public SingularAttributeSourceImpl(MappedAttribute attribute) {
		this(attribute, "");
	}

	public SingularAttributeSourceImpl(MappedAttribute attribute, String parentPath) {
		this.attribute = attribute;
		this.type = new HibernateTypeSourceImpl( attribute );
		this.attributePath = StringHelper.isEmpty( parentPath ) ? attribute.getName() : parentPath + "." + attribute.getName();
	}

	@Override
	public void applyAssociationOverride(Map<String, AssociationOverride> associationOverrideMap) {
		//doing nothing here
	}

	@Override
	public void applyAttributeOverride(Map<String, AttributeOverride> attributeOverrideMap) {
		this.attributeOverride = attributeOverrideMap.get( attributePath );
	}

	@Override
	public HibernateTypeSource getTypeInformation() {
		return type;
	}

	@Override
	public String getPropertyAccessorName() {
		return attribute.getAccessType();
	}

	@Override
	public PropertyGeneration getGeneration() {
		return attribute.getPropertyGeneration();
	}

	@Override
	public boolean isLazy() {
		return attribute.isLazy();
	}

	@Override
	public SingularAttributeBinding.NaturalIdMutability getNaturalIdMutability() {
		return attribute.getNaturalIdMutability();
	}

	@Override
	public boolean isIncludedInOptimisticLocking() {
		return attribute.isOptimisticLockable();
	}

	@Override
	public String getName() {
		return attribute.getName();
	}

	@Override
	public String getContainingTableName() {
		return null;
	}

	private final ValueHolder<List<RelationalValueSource>> relationalValueSources = new ValueHolder<List<RelationalValueSource>>(
			new ValueHolder.DeferredInitializer<List<RelationalValueSource>>() {
				@Override
				public List<RelationalValueSource> initialize() {
					List<RelationalValueSource> valueSources = new ArrayList<RelationalValueSource>();
					if ( attributeOverride != null ) {
						attributeOverride.apply( attribute );
					}
					boolean hasDefinedColumnSource = !attribute.getColumnValues().isEmpty();
					if ( hasDefinedColumnSource ) {
						for ( Column columnValues : attribute.getColumnValues() ) {

							valueSources.add( new ColumnSourceImpl( attribute, columnValues ) );
						}
					}
					else if ( attribute.getFormulaValue() != null ) {
						valueSources.add( new DerivedValueSourceImpl( attribute.getFormulaValue() ) );
					}
					else if ( attribute instanceof BasicAttribute ) {
						//for column transformer
						BasicAttribute basicAttribute = BasicAttribute.class.cast( attribute );
						if ( basicAttribute.getCustomReadFragment() != null && basicAttribute.getCustomWriteFragment() != null ) {

							valueSources.add( new ColumnSourceImpl( attribute, null ) );
						}
					}
					return valueSources;
				}
			}
	);

	/**
	 * very ugly, can we just return the columnSourceImpl anyway?
	 */
	@Override
	public List<RelationalValueSource> relationalValueSources() {
		return relationalValueSources.getValue();
	}

	@Override
	public boolean isVirtualAttribute() {
		return false;
	}

	@Override
	public boolean isSingular() {
		return true;
	}

	@Override
	public Nature getNature() {
		return Nature.BASIC;
	}

	@Override
	public Iterable<MetaAttributeSource> getMetaAttributeSources() {
		return Collections.emptySet();
	}

	@Override
	public boolean areValuesIncludedInInsertByDefault() {
		return attribute.isInsertable();
	}

	@Override
	public boolean areValuesIncludedInUpdateByDefault() {
		return !attribute.isId() && attribute.isUpdatable();
	}

	@Override
	public boolean areValuesNullableByDefault() {
		return !attribute.isId() && attribute.isOptional();
	}
}


