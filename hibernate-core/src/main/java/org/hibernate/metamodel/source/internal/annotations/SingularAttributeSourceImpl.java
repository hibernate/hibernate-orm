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
package org.hibernate.metamodel.source.internal.annotations;

import java.util.Collection;
import java.util.Collections;

import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.source.internal.annotations.attribute.PersistentAttribute;
import org.hibernate.metamodel.source.internal.annotations.attribute.SingularAttribute;
import org.hibernate.metamodel.source.spi.HibernateTypeSource;
import org.hibernate.metamodel.source.spi.SingularAttributeSource;
import org.hibernate.metamodel.source.spi.ToolingHintSource;
import org.hibernate.metamodel.spi.NaturalIdMutability;

/**
 * @author Hardy Ferentschik
 */
public abstract class SingularAttributeSourceImpl implements SingularAttributeSource, AnnotationAttributeSource {
	private final SingularAttribute attribute;
	private final HibernateTypeSource type;


	public SingularAttributeSourceImpl(SingularAttribute attribute) {
		this.attribute = attribute;
		this.type = new HibernateTypeSourceImpl( attribute );
	}

	@Override
	public PersistentAttribute getAnnotatedAttribute() {
		return attribute;
	}

	@Override
	public HibernateTypeSource getTypeInformation() {
		return type;
	}

	@Override
	public String getPropertyAccessorName() {
		return attribute.getAccessorStrategy();
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
	public NaturalIdMutability getNaturalIdMutability() {
		return attribute.getNaturalIdMutability();
	}

	@Override
	public boolean isIncludedInOptimisticLocking() {
		return attribute.isIncludeInOptimisticLocking();
	}

	@Override
	public String getName() {
		return attribute.getName();
	}

	@Override
	public String getContainingTableName() {
		return null;
	}

//	/**
//	 * very ugly, can we just return the columnSourceImpl anyway?
//	 */
//	@Override
//	public List<RelationalValueSource> relationalValueSources() {
//		if ( relationalValueSources == null ) {
//			relationalValueSources = buildRelationalValueSources();
//		}
//		return relationalValueSources;
//	}
//
//	private List<RelationalValueSource> buildRelationalValueSources() {
//		List<RelationalValueSource> results = new ArrayList<RelationalValueSource>();
//
////		if ( attributeOverride != null ) {
////			attributeOverride.apply( attribute );
////		}
//
//		boolean hasDefinedColumnSource = !attribute.getColumnValues().isEmpty();
//		if ( hasDefinedColumnSource ) {
//			for ( Column columnValues : attribute.getColumnValues() ) {
//				results.add( new ColumnSourceImpl( attribute, columnValues ) );
//			}
//		}
//		else if ( attribute.getFormulaValue() != null ) {
//			results.add( new DerivedValueSourceImpl( attribute.getFormulaValue() ) );
//		}
//		else if ( attribute instanceof BasicAttribute ) {
//			//for column transformer
//			BasicAttribute simpleAttribute = BasicAttribute.class.cast( attribute );
//			if ( simpleAttribute.getCustomReadFragment() != null && simpleAttribute.getCustomWriteFragment() != null ) {
//				results.add( new ColumnSourceImpl( attribute, null ) );
//			}
//		}
//		return results;
//	}

	@Override
	public boolean isVirtualAttribute() {
		return false;
	}

	@Override
	public boolean isSingular() {
		return true;
	}

	@Override
	public Collection<? extends ToolingHintSource> getToolingHintSources() {
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


