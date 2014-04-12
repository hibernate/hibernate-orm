/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal.binder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.TruthValue;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.source.internal.annotations.attribute.SingularAssociationAttribute;
import org.hibernate.metamodel.source.spi.ColumnSource;
import org.hibernate.metamodel.source.spi.DerivedValueSource;
import org.hibernate.metamodel.source.spi.RelationalValueSource;
import org.hibernate.metamodel.source.spi.RelationalValueSourceContainer;
import org.hibernate.metamodel.source.spi.SingularAttributeSource;
import org.hibernate.metamodel.spi.NaturalIdMutability;
import org.hibernate.metamodel.spi.binding.AttributeBindingContainer;
import org.hibernate.metamodel.spi.binding.RelationalValueBinding;
import org.hibernate.metamodel.spi.domain.Attribute;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.DerivedValue;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.metamodel.spi.relational.Value;

/**
 * @author Gail Badner
 */
public class RelationalValueBindingHelper {

	private final BinderRootContext helperContext;

	public RelationalValueBindingHelper(BinderRootContext helperContext) {
		this.helperContext = helperContext;
	}

	public boolean hasDerivedValue(List<RelationalValueBinding> relationalValueBindings) {
		for ( RelationalValueBinding relationalValueBinding : relationalValueBindings ) {
			if (relationalValueBinding.isDerived() ) {
				return true;
			}
		}
		return false;
	}

	public List<RelationalValueBinding> createRelationalValueBindings(
			final AttributeBindingContainer attributeBindingContainer,
			final RelationalValueSourceContainer valueSourceContainer,
			final Attribute attribute,
			final TableSpecification defaultTable,
			final boolean forceNonNullable) {
		final Binder.DefaultNamingStrategy defaultNamingStrategy = new Binder.DefaultNamingStrategy() {
			@Override
			public String defaultName(NamingStrategy namingStrategy) {
				return namingStrategy.propertyToColumnName( attribute.getName() );
			}
		};
		return createRelationalValueBindings(
				attributeBindingContainer,
				valueSourceContainer,
				defaultTable,
				Collections.singletonList( defaultNamingStrategy ),
				forceNonNullable
		);
	}

	public List<RelationalValueBinding> createRelationalValueBindings(
			final AttributeBindingContainer attributeBindingContainer,
			final RelationalValueSourceContainer valueSourceContainer,
			final TableSpecification defaultTable,
			final List<Binder.DefaultNamingStrategy> defaultNameStrategies,
			final boolean forceNonNullable) {
		final List<RelationalValueBinding> valueBindings = new ArrayList<RelationalValueBinding>();
		final NaturalIdMutability naturalIdMutability;
		if ( SingularAttributeSource.class.isInstance( valueSourceContainer ) &&
				SingularAttributeSource.class.cast( valueSourceContainer ).getNaturalIdMutability() != null ) {
			naturalIdMutability = SingularAttributeSource.class.cast( valueSourceContainer ).getNaturalIdMutability();
		}
		else {
			naturalIdMutability = NaturalIdMutability.NOT_NATURAL_ID;
		}
		final boolean isNaturalId = naturalIdMutability != NaturalIdMutability.NOT_NATURAL_ID;
		final boolean isImmutableNaturalId = isNaturalId && ( naturalIdMutability == NaturalIdMutability.IMMUTABLE );
		final boolean reallyForceNonNullable = forceNonNullable ; //|| isNaturalId; todo is a natural id column should be not nullable?

		if ( valueSourceContainer.relationalValueSources().isEmpty() ) {
			for ( Binder.DefaultNamingStrategy defaultNameStrategy : defaultNameStrategies ) {
				final Column column = helperContext.tableHelper().locateOrCreateColumn(
						defaultTable,
						null,
						new DefaultColumnNamingStrategyHelper( defaultNameStrategy )
				);
				column.setNullable( !reallyForceNonNullable && valueSourceContainer.areValuesNullableByDefault() );
				if ( isNaturalId ) {
					helperContext.naturalIdUniqueKeyHelper().addUniqueConstraintForNaturalIdColumn( defaultTable, column );
				}
				valueBindings.add(
						new RelationalValueBinding(
								defaultTable,
								column,
								valueSourceContainer.areValuesIncludedInInsertByDefault(),
								valueSourceContainer.areValuesIncludedInUpdateByDefault() && !isImmutableNaturalId
						)
				);
			}

		}
		else {
			for ( int i = 0 ; i <  valueSourceContainer.relationalValueSources().size(); i++ ) {
				final RelationalValueSource valueSource = valueSourceContainer.relationalValueSources().get( i );
				final TableSpecification table =
						valueSource.getContainingTableName() == null
								? defaultTable
								: attributeBindingContainer.seekEntityBinding()
								.locateTable( valueSource.getContainingTableName() );
				if ( valueSource.getNature() == RelationalValueSource.Nature.COLUMN ) {
					final ColumnSource columnSource = (ColumnSource) valueSource;
					final DefaultColumnNamingStrategyHelper defaultColumnNamingStrategyHelper =
							new DefaultColumnNamingStrategyHelper(
									StringHelper.isEmpty( columnSource.getName() ) ?
											defaultNameStrategies.get( i ) :
											null
							);
					Column column = helperContext.tableHelper().locateOrCreateColumn(
							table,
							columnSource,
							defaultColumnNamingStrategyHelper,
							reallyForceNonNullable,
							valueSourceContainer.areValuesNullableByDefault()
					);
					if ( isNaturalId ) {
						helperContext.naturalIdUniqueKeyHelper().addUniqueConstraintForNaturalIdColumn( table, column );
					}
					final boolean isIncludedInInsert =
							TruthValue.toBoolean(
									columnSource.isIncludedInInsert(),
									valueSourceContainer.areValuesIncludedInInsertByDefault()
							);
					final boolean isIncludedInUpdate =
							TruthValue.toBoolean(
									columnSource.isIncludedInUpdate(),
									valueSourceContainer.areValuesIncludedInUpdateByDefault()
							);
					valueBindings.add(
							new RelationalValueBinding(
									table,
									column,
									isIncludedInInsert,
									!isImmutableNaturalId && isIncludedInUpdate
							)
					);
				}
				else {
					final DerivedValue derivedValue =
							table.locateOrCreateDerivedValue( ( (DerivedValueSource) valueSource ).getExpression() );
					valueBindings.add( new RelationalValueBinding( table, derivedValue ) );
				}
			}
		}
		return valueBindings;
	}

	public List<RelationalValueBinding> bindInverseRelationalValueBindings(
			TableSpecification table,
			List<? extends Value> values) {
		final List<RelationalValueBinding> relationalValueBindings =
				new ArrayList<RelationalValueBinding>( values.size() );
		for ( Value value : values ) {
			final RelationalValueBinding relationalValueBinding =
					value.getValueType() == Value.ValueType.COLUMN ?
							new RelationalValueBinding( table, (Column) value, false, false ) :
							new RelationalValueBinding( table, (DerivedValue) value );
			relationalValueBindings.add( relationalValueBinding );
		}
		return relationalValueBindings;
	}
}
