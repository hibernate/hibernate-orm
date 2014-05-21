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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.source.internal.annotations.attribute.Column;
import org.hibernate.metamodel.source.internal.annotations.attribute.PluralAttribute;
import org.hibernate.metamodel.source.spi.PluralAttributeKeySource;
import org.hibernate.metamodel.source.spi.RelationalValueSource;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.metamodel.spi.relational.Value;

/**
 * @author Hardy Ferentschik
 * @author Strong Liu <stliu@hibernate.org>
 */
public class PluralAttributeKeySourceImpl implements PluralAttributeKeySource {
	private final PluralAttribute attribute;
	private final boolean isCascadeDeleteEnabled;

	public PluralAttributeKeySourceImpl(PluralAttribute attribute) {
		this.attribute = attribute;
		this.isCascadeDeleteEnabled = attribute.getOnDeleteAction() == OnDeleteAction.CASCADE;
	}
	@Override
	public boolean isCascadeDeleteEnabled() {
		return isCascadeDeleteEnabled;
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
	public String getExplicitForeignKeyName() {
		return attribute.getExplicitForeignKeyName();
	}

	@Override
	public boolean createForeignKeyConstraint() {
		return attribute.createForeignKeyConstraint();
	}

	@Override
	public JoinColumnResolutionDelegate getForeignKeyTargetColumnResolutionDelegate() {
		if ( attribute.isInverse() ) {
			throw new IllegalStateException( "Cannot determine foreign key information because association is not the owner." );
		}
		for ( Column joinColumn : attribute.getJoinColumnValues() ) {
			if ( StringHelper.isNotEmpty( joinColumn.getReferencedColumnName() ) ) {
				return new JoinColumnResolutionDelegateImpl( attribute );
			}
		}
		return null;
	}

	@Override
	public List<RelationalValueSource> relationalValueSources() {
		List<Column> joinClumnValues = attribute.getJoinColumnValues();
		if ( joinClumnValues.isEmpty() ) {
			return Collections.emptyList();
		}
		List<RelationalValueSource> result = new ArrayList<RelationalValueSource>( joinClumnValues.size() );
		for ( Column joinColumn : joinClumnValues ) {
			result.add( new ColumnSourceImpl( joinColumn ) );
		}
		return result;
	}

	public static class JoinColumnResolutionDelegateImpl implements JoinColumnResolutionDelegate {
		private final PluralAttribute attribute;

		public JoinColumnResolutionDelegateImpl(PluralAttribute attribute) {
			this.attribute = attribute;
		}

		@Override
		public List<? extends Value> getJoinColumns(JoinColumnResolutionContext context) {
			List<Column> joinColumnValues = attribute.getJoinColumnValues();
			if ( joinColumnValues.isEmpty() ) {
				return null;
			}
			List<Value> result = new ArrayList<Value>( joinColumnValues.size() );
			for ( Column column : attribute.getJoinColumnValues() ) {
				result.add( context.resolveColumn( column.getReferencedColumnName(), null, null, null ) );
			}
			return result;
		}

		@Override
		public String getReferencedAttributeName() {
			return null;
		}

		@Override
		public TableSpecification getReferencedTable(JoinColumnResolutionContext context) {
			return context.resolveTableForAttribute( null );
		}
	}
}
