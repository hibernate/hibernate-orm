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
import java.util.Map;
import java.util.Set;

import org.hibernate.MappingException;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.metamodel.domain.Attribute;
import org.hibernate.metamodel.domain.MetaAttribute;
import org.hibernate.metamodel.relational.Column;
import org.hibernate.metamodel.relational.DerivedValue;
import org.hibernate.metamodel.relational.SimpleValue;
import org.hibernate.metamodel.relational.Size;
import org.hibernate.metamodel.relational.TableSpecification;
import org.hibernate.metamodel.relational.Tuple;
import org.hibernate.metamodel.relational.Value;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public abstract class AbstractAttributeBinding implements AttributeBinding {
	public static interface DomainState {
		HibernateTypeDescriptor getHibernateTypeDescriptor();
		Attribute getAttribute();
		boolean isLazy();
		String getPropertyAccessorName();
		boolean isAlternateUniqueKey();
		String getCascade();
		boolean isOptimisticLockable();
		String getNodeName();
		Map<String, MetaAttribute> getMetaAttributes(EntityBinding entityBinding);
	}

	private final HibernateTypeDescriptor hibernateTypeDescriptor = new HibernateTypeDescriptor();
	private final EntityBinding entityBinding;
	private final Set<EntityReferencingAttributeBinding> entityReferencingAttributeBindings =
			new HashSet<EntityReferencingAttributeBinding>();

	private Attribute attribute;
	private Value value;

	private boolean isLazy;
	private String propertyAccessorName;
	private boolean isAlternateUniqueKey;
	private String cascade;
	private boolean optimisticLockable;

	// DOM4J specific...
	private String nodeName;

	private Map<String, MetaAttribute> metaAttributes;

	protected AbstractAttributeBinding(EntityBinding entityBinding) {
		this.entityBinding = entityBinding;
	}

	public void initialize(DomainState state) {
		hibernateTypeDescriptor.initialize( state.getHibernateTypeDescriptor() );
		attribute = state.getAttribute();
		isLazy = state.isLazy();
		propertyAccessorName = state.getPropertyAccessorName();
		isAlternateUniqueKey = state.isAlternateUniqueKey();
		cascade = state.getCascade();
		optimisticLockable = state.isOptimisticLockable();
		nodeName = state.getNodeName();
		metaAttributes = state.getMetaAttributes( entityBinding );
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

	protected boolean forceNonNullable() {
		return false;
	}

	protected boolean forceUnique() {
		return false;
	}

	protected void initializeColumnValue(ColumnRelationalState state) {
		Column columnValue = createColumn( state );
		setValue( columnValue );
	}

	private Column createColumn(ColumnRelationalState state) {
		final String explicitName = state.getExplicitColumnName();
		final String logicalColumnName = state.getNamingStrategy().logicalColumnName( explicitName, getAttribute().getName() );
		final TableSpecification table = getEntityBinding().getBaseTable();
		final String columnName =
				explicitName == null ?
						state.getNamingStrategy().propertyToColumnName( getAttribute().getName() ) :
						state.getNamingStrategy().columnName( explicitName );
// todo : find out the purpose of these logical bindings
//			mappings.addColumnBinding( logicalColumnName, column, table );
		Column columnValue = table.createColumn( columnName );
		columnValue.getSize().initialize( state.getSize() );
		columnValue.setNullable( ! forceNonNullable() &&  state.isNullable() );
		columnValue.setUnique( ! forceUnique() && state.isUnique()  );
		columnValue.setCheckCondition( state.getCheckCondition() );
		columnValue.setDefaultValue( state.getDefault() );
		columnValue.setSqlType( state.getSqlType() );
		columnValue.setWriteFragment( state.getCustomWriteFragment() );
		columnValue.setReadFragment( state.getCustomReadFragment() );
		columnValue.setComment( state.getComment() );
		for ( String uniqueKey : state.getUniqueKeys() ) {
			table.getOrCreateUniqueKey( uniqueKey ).addColumn( columnValue );
		}
		for ( String index : state.getIndexes() ) {
			table.getOrCreateIndex( index ).addColumn( columnValue );
		}
		return columnValue;
	}

	public final void initialize(RelationalState state) {
		if ( SingleValueRelationalState.class.isInstance( state ) ) {
			initializeSingleValue( SingleValueRelationalState.class.cast( state )  );
		}
		else if ( SimpleTupleRelationalState.class.isInstance( state ) ) {
			initializeTupleValue( SimpleTupleRelationalState.class.cast( state ).getRelationalStates() );
		}
	}


	public final <T extends DerivedRelationalState> void initializeDerivedValue(T state) {
		value = createDerivedValue( state );
	}

	private DerivedValue createDerivedValue(DerivedRelationalState state) {
		return getEntityBinding().getBaseTable().createDerivedValue( state.getFormula() );
	}

	public final void initializeSingleValue(SingleValueRelationalState state) {
		value = createSingleValue( state );
	}

	protected SimpleValue createSingleValue(SingleValueRelationalState state) {
		if ( state instanceof ColumnRelationalState ) {
			return createColumn( ColumnRelationalState.class.cast( state ) );
		}
		else if ( state instanceof DerivedRelationalState ) {
			return createDerivedValue( DerivedRelationalState.class.cast( state ) );
		}
		else {
			throw new MappingException( "unknown relational state:" + state.getClass().getName() );
		}
	}

	protected final void initializeTupleValue(Set<SingleValueRelationalState> singleValueStates) {
		if ( singleValueStates.size() == 0 ) {
			throw new MappingException( "Tuple state does not contain any values." );
		}
		if ( singleValueStates.size() == 1 ) {
			initializeSingleValue( singleValueStates.iterator().next() );
		}
		else {
			Tuple tuple = getEntityBinding().getBaseTable().createTuple(  "[" + getAttribute().getName() + "]" );
			for ( SingleValueRelationalState singleValueState : singleValueStates ) {
				tuple.addValue( createSingleValue( singleValueState ) );
			}
			value = tuple;
		}
	}

	@Override
	public Value getValue() {
		return value;
	}

	protected void setValue(Value value) {
		this.value = value;
	}

	@Override
	public HibernateTypeDescriptor getHibernateTypeDescriptor() {
		return hibernateTypeDescriptor;
	}

	public String getCascade() {
		return cascade;
	}

	public boolean isOptimisticLockable() {
		return optimisticLockable;
	}

	public String getNodeName() {
		return nodeName;
	}

	@Override
	public Map<String, MetaAttribute> getMetaAttributes() {
		return metaAttributes;
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
	public TableSpecification getTable() {
		return getValue().getTable();
	}

	@Override
	public String getPropertyAccessorName() {
		return propertyAccessorName;
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
			tmp.add( ! ( simpleValue instanceof DerivedValue ) );
		}
		boolean[] rtn = new boolean[ tmp.size() ];
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

	protected void setLazy(boolean isLazy) {
		this.isLazy = isLazy;
	}

	public void addEntityReferencingAttributeBinding(EntityReferencingAttributeBinding referencingAttributeBinding) {
		entityReferencingAttributeBindings.add( referencingAttributeBinding );
	}

	public Set<EntityReferencingAttributeBinding> getEntityReferencingAttributeBindings() {
		return Collections.unmodifiableSet( entityReferencingAttributeBindings );
	}

	public void validate() {
		if ( ! entityReferencingAttributeBindings.isEmpty() ) {
			// TODO; validate that this AttributeBinding can be a target of an entity reference
			// (e.g., this attribute is the primary key or there is a unique-key)
			// can a unique attribute be used as a target? if so, does it need to be non-null?
		}
	}

	public static interface RelationalState {}

	public static interface SingleValueRelationalState extends RelationalState {}

	public static interface ColumnRelationalState extends SingleValueRelationalState {
		NamingStrategy getNamingStrategy();
		String getExplicitColumnName();
		boolean isUnique();
		Size getSize();
		boolean isNullable();
		String getCheckCondition();
		String getDefault();
		String getSqlType();
		String getCustomWriteFragment();
		String getCustomReadFragment();
		String getComment();
		Set<String> getUniqueKeys();
		Set<String> getIndexes();
	}

	public static interface DerivedRelationalState extends SingleValueRelationalState {
		String getFormula();
	}

	public static interface SimpleTupleRelationalState extends TupleRelationalState<SingleValueRelationalState> {
	}

	public static interface TupleRelationalState<T extends RelationalState> extends RelationalState{
		Set<T> getRelationalStates();
	}
}
