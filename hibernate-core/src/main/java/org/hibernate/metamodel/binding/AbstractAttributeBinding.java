/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.binding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.MappingException;
import org.hibernate.metamodel.binder.source.MetaAttributeContext;
import org.hibernate.metamodel.binding.state.AttributeBindingState;
import org.hibernate.metamodel.domain.Attribute;
import org.hibernate.metamodel.relational.Column;
import org.hibernate.metamodel.relational.DerivedValue;
import org.hibernate.metamodel.relational.SimpleValue;
import org.hibernate.metamodel.relational.Tuple;
import org.hibernate.metamodel.relational.Value;
import org.hibernate.metamodel.relational.state.SimpleValueRelationalState;
import org.hibernate.metamodel.relational.state.TupleRelationalState;
import org.hibernate.metamodel.relational.state.ValueCreator;
import org.hibernate.metamodel.relational.state.ValueRelationalState;

/**
 * Basic support for {@link AttributeBinding} implementors
 *
 * @author Steve Ebersole
 */
public abstract class AbstractAttributeBinding implements AttributeBinding {
	private final EntityBinding entityBinding;

	private final HibernateTypeDescriptor hibernateTypeDescriptor = new HibernateTypeDescriptor();
	private final Set<EntityReferencingAttributeBinding> entityReferencingAttributeBindings = new HashSet<EntityReferencingAttributeBinding>();

	private Attribute attribute;
	private Value value;

	private boolean isLazy;
	private String propertyAccessorName;
	private boolean isAlternateUniqueKey;
	private Set<CascadeType> cascadeTypes;
	private boolean optimisticLockable;

	private MetaAttributeContext metaAttributeContext;

	protected AbstractAttributeBinding(EntityBinding entityBinding) {
		this.entityBinding = entityBinding;
	}

	protected void initialize(AttributeBindingState state) {
		hibernateTypeDescriptor.setTypeName( state.getTypeName() );
		hibernateTypeDescriptor.setTypeParameters( state.getTypeParameters() );
		isLazy = state.isLazy();
		propertyAccessorName = state.getPropertyAccessorName();
		isAlternateUniqueKey = state.isAlternateUniqueKey();
		cascadeTypes = state.getCascadeTypes();
		optimisticLockable = state.isOptimisticLockable();
		metaAttributeContext = state.getMetaAttributeContext();
	}

	@Override
	public EntityBinding getEntityBinding() {
		return entityBinding;
	}

	@Override
	public Attribute getAttribute() {
		return attribute;
	}

	protected void setAttribute(Attribute attribute) {
		this.attribute = attribute;
	}

	public void setValue(Value value) {
		this.value = value;
	}

	protected boolean forceNonNullable() {
		return false;
	}

	protected boolean forceUnique() {
		return false;
	}

	protected final boolean isPrimaryKey() {
		return this == getEntityBinding().getEntityIdentifier().getValueBinding();
	}

	protected void initializeValueRelationalState(ValueRelationalState state) {
		// TODO: change to have ValueRelationalState generate the value
		value = ValueCreator.createValue(
				getEntityBinding().getBaseTable(),
				getAttribute().getName(),
				state,
				forceNonNullable(),
				forceUnique()
		);
		// TODO: not sure I like this here...
		if ( isPrimaryKey() ) {
			if ( SimpleValue.class.isInstance( value ) ) {
				if ( !Column.class.isInstance( value ) ) {
					// this should never ever happen..
					throw new MappingException( "Simple ID is not a column." );
				}
				entityBinding.getBaseTable().getPrimaryKey().addColumn( Column.class.cast( value ) );
			}
			else {
				for ( SimpleValueRelationalState val : TupleRelationalState.class.cast( state )
						.getRelationalStates() ) {
					if ( Column.class.isInstance( val ) ) {
						entityBinding.getBaseTable().getPrimaryKey().addColumn( Column.class.cast( val ) );
					}
				}
			}
		}
	}

	@Override
	public Value getValue() {
		return value;
	}

	@Override
	public HibernateTypeDescriptor getHibernateTypeDescriptor() {
		return hibernateTypeDescriptor;
	}

	public Set<CascadeType> getCascadeTypes() {
		return cascadeTypes;
	}

	public boolean isOptimisticLockable() {
		return optimisticLockable;
	}

	@Override
	public MetaAttributeContext getMetaAttributeContext() {
		return metaAttributeContext;
	}

	@Override
	public int getValuesSpan() {
		if ( value == null ) {
			return 0;
		}
		else if ( value instanceof Tuple ) {
			return ( ( Tuple ) value ).valuesSpan();
		}
		else {
			return 1;
		}
	}


	@Override
	public Iterable<SimpleValue> getValues() {
		return value == null
				? Collections.<SimpleValue>emptyList()
				: value instanceof Tuple
				? ( (Tuple) value ).values()
				: Collections.singletonList( (SimpleValue) value );
	}

	@Override
	public String getPropertyAccessorName() {
		return propertyAccessorName;
	}

	@Override
	public boolean isBasicPropertyAccessor() {
		return propertyAccessorName==null || "property".equals( propertyAccessorName );
	}


	@Override
	public boolean hasFormula() {
		for ( SimpleValue simpleValue : getValues() ) {
			if ( simpleValue instanceof DerivedValue ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isAlternateUniqueKey() {
		return isAlternateUniqueKey;
	}

	public void setAlternateUniqueKey(boolean alternateUniqueKey) {
		this.isAlternateUniqueKey = alternateUniqueKey;
	}

	@Override
	public boolean isNullable() {
		for ( SimpleValue simpleValue : getValues() ) {
			if ( simpleValue instanceof DerivedValue ) {
				return true;
			}
			Column column = (Column) simpleValue;
			if ( column.isNullable() ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean[] getColumnInsertability() {
		List<Boolean> tmp = new ArrayList<Boolean>();
		for ( SimpleValue simpleValue : getValues() ) {
			tmp.add( !( simpleValue instanceof DerivedValue ) );
		}
		boolean[] rtn = new boolean[tmp.size()];
		int i = 0;
		for ( Boolean insertable : tmp ) {
			rtn[i++] = insertable.booleanValue();
		}
		return rtn;
	}

	@Override
	public boolean[] getColumnUpdateability() {
		return getColumnInsertability();
	}

	@Override
	public boolean isLazy() {
		return isLazy;
	}

	public void setLazy(boolean isLazy) {
		this.isLazy = isLazy;
	}

	public void addEntityReferencingAttributeBinding(EntityReferencingAttributeBinding referencingAttributeBinding) {
		entityReferencingAttributeBindings.add( referencingAttributeBinding );
	}

	public Set<EntityReferencingAttributeBinding> getEntityReferencingAttributeBindings() {
		return Collections.unmodifiableSet( entityReferencingAttributeBindings );
	}

	public void validate() {
		if ( !entityReferencingAttributeBindings.isEmpty() ) {
			// TODO; validate that this AttributeBinding can be a target of an entity reference
			// (e.g., this attribute is the primary key or there is a unique-key)
			// can a unique attribute be used as a target? if so, does it need to be non-null?
		}
	}
}
