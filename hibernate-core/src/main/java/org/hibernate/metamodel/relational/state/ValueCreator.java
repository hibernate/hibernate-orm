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
package org.hibernate.metamodel.relational.state;

import org.hibernate.MappingException;
import org.hibernate.metamodel.relational.Column;
import org.hibernate.metamodel.relational.DerivedValue;
import org.hibernate.metamodel.relational.SimpleValue;
import org.hibernate.metamodel.relational.TableSpecification;
import org.hibernate.metamodel.relational.Tuple;
import org.hibernate.metamodel.relational.Value;

/**
 * @author Gail Badner
 */
public class ValueCreator {

	public static Column createColumn(TableSpecification table,
									  String attributeName,
									  ColumnRelationalState state,
									  boolean forceNonNullable,
									  boolean forceUnique
	) {
		final String explicitName = state.getExplicitColumnName();
		final String logicalColumnName = state.getNamingStrategy().logicalColumnName( explicitName, attributeName );
		final String columnName =
				explicitName == null ?
						state.getNamingStrategy().propertyToColumnName( attributeName ) :
						state.getNamingStrategy().columnName( explicitName );
// todo : find out the purpose of these logical bindings
//			mappings.addColumnBinding( logicalColumnName, column, table );

		if ( columnName == null ) {
			throw new IllegalArgumentException( "columnName must be non-null." );
		}
		Column value = table.createColumn( columnName );
		value.initialize( state, forceNonNullable, forceUnique );
		return value;
	}

	public static DerivedValue createDerivedValue(TableSpecification table,
												  DerivedValueRelationalState state) {
		return table.createDerivedValue( state.getFormula() );
	}

	public static SimpleValue createSimpleValue(TableSpecification table,
												String attributeName,
												SimpleValueRelationalState state,
												boolean forceNonNullable,
												boolean forceUnique
	) {
		if ( state instanceof ColumnRelationalState ) {
			ColumnRelationalState columnRelationalState = ColumnRelationalState.class.cast( state );
			return createColumn( table, attributeName, columnRelationalState, forceNonNullable, forceUnique );
		}
		else if ( state instanceof DerivedValueRelationalState ) {
			return createDerivedValue( table, DerivedValueRelationalState.class.cast( state ) );
		}
		else {
			throw new MappingException( "unknown relational state:" + state.getClass().getName() );
		}
	}

	public static Tuple createTuple(TableSpecification table,
									String attributeName,
									TupleRelationalState state,
									boolean forceNonNullable,
									boolean forceUnique
	) {
		Tuple tuple = table.createTuple( "[" + attributeName + "]" );
		for ( SimpleValueRelationalState valueState : state.getRelationalStates() ) {
			tuple.addValue( createSimpleValue( table, attributeName, valueState, forceNonNullable, forceUnique ) );
		}
		return tuple;
	}

	public static Value createValue(TableSpecification table,
									String attributeName,
									ValueRelationalState state,
									boolean forceNonNullable,
									boolean forceUnique) {
		Value value = null;
		if ( SimpleValueRelationalState.class.isInstance( state ) ) {
			value = createSimpleValue(
					table,
					attributeName,
					SimpleValueRelationalState.class.cast( state ),
					forceNonNullable,
					forceUnique
			);
		}
		else if ( TupleRelationalState.class.isInstance( state ) ) {
			value = createTuple(
					table,
					attributeName,
					TupleRelationalState.class.cast( state ),
					forceNonNullable,
					forceUnique
			);
		}
		else {
			throw new MappingException( "Unexpected type of RelationalState" + state.getClass().getName() );
		}
		return value;
	}
}
