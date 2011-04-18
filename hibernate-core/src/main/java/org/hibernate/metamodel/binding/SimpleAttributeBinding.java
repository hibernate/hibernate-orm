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

import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.MappingException;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.mapping.PropertyGeneration;
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
public class SimpleAttributeBinding extends SingularAttributeBinding {
	public static interface DomainState extends SingularAttributeBinding.DomainState {
		public PropertyGeneration getPropertyGeneration();
	}

	public static interface SingleValueRelationalState {}
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

	public static interface TupleRelationalState {
		Set<SingleValueRelationalState> getSingleValueRelationalStates();
	}

	private PropertyGeneration generation;

	SimpleAttributeBinding(EntityBinding entityBinding, boolean forceNonNullable, boolean forceUnique) {
		super( entityBinding, forceNonNullable, forceUnique );
	}

	public final void initialize(DomainState state) {
		super.initialize( state );
		generation = state.getPropertyGeneration();
	}

	public final void initializeColumnValue(ColumnRelationalState state) {
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

	private boolean isUnique(ColumnRelationalState state) {
		return isPrimaryKey() || state.isUnique();

	}

	public final <T extends DerivedRelationalState> void initializeDerivedValue(T state) {
		setValue( createDerivedValue( state ) );
	}

	private DerivedValue createDerivedValue(DerivedRelationalState state) {
		return getEntityBinding().getBaseTable().createDerivedValue( state.getFormula() );
	}

	public final void initializeTupleValue(TupleRelationalState state) {
		if ( state.getSingleValueRelationalStates().size() == 0 ) {
			throw new MappingException( "Tuple state does not contain any values." );
		}
		if ( state.getSingleValueRelationalStates().size() == 1 ) {
			setValue( createSingleValue( state.getSingleValueRelationalStates().iterator().next() ) );
		}
		else {
			Tuple tuple = getEntityBinding().getBaseTable().createTuple(  "[" + getAttribute().getName() + "]" );
			for ( SingleValueRelationalState singleValueState : state.getSingleValueRelationalStates() ) {
				tuple.addValue( createSingleValue( singleValueState ) );
			}
			setValue( tuple );
		}
	}

	private SimpleValue createSingleValue(SingleValueRelationalState state) {
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

	@Override
	public boolean isSimpleValue() {
		return true;
	}

	private boolean isPrimaryKey() {
		return this == getEntityBinding().getEntityIdentifier().getValueBinding();
	}

	public PropertyGeneration getGeneration() {
		return generation;
	}
}
