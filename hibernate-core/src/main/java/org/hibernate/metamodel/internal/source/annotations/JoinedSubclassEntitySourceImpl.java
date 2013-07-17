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
import java.util.List;

import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.internal.source.annotations.attribute.PrimaryKeyJoinColumn;
import org.hibernate.metamodel.internal.source.annotations.entity.EntityClass;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.metamodel.spi.source.ColumnSource;
import org.hibernate.metamodel.spi.source.EntitySource;
import org.hibernate.metamodel.spi.source.JoinedSubclassEntitySource;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class JoinedSubclassEntitySourceImpl extends SubclassEntitySourceImpl implements JoinedSubclassEntitySource {
	private final List<ColumnSource> columnSources;
	private final JoinColumnResolutionDelegate fkColumnResolutionDelegate;

	public JoinedSubclassEntitySourceImpl(EntityClass entityClass, EntitySource container) {
		super( entityClass, container );
		// todo : following normal annotation idiom for source, we probably want to move this stuff up to EntityClass...
		columnSources = new ArrayList<ColumnSource>();
		final List<String> targetColumnNames = new ArrayList<String>();
		boolean hadNamedTargetColumnReferences = false;
		if ( CollectionHelper.isNotEmpty( entityClass.getJoinedSubclassPrimaryKeyJoinColumnSources() ) ) {
			for ( PrimaryKeyJoinColumn primaryKeyJoinColumnSource : entityClass.getJoinedSubclassPrimaryKeyJoinColumnSources() ) {
				columnSources.add(
						new ColumnSourceImpl( primaryKeyJoinColumnSource )
				);
				targetColumnNames.add( primaryKeyJoinColumnSource.getReferencedColumnName() );
				if ( primaryKeyJoinColumnSource.getReferencedColumnName() != null ) {
					hadNamedTargetColumnReferences = true;
				}
			}
		}

		this.fkColumnResolutionDelegate = !hadNamedTargetColumnReferences
				? null
				: new JoinColumnResolutionDelegateImpl( targetColumnNames );
	}

	@Override
	public boolean isCascadeDeleteEnabled() {
		return getEntityClass().getOnDeleteAction() != null && getEntityClass().getOnDeleteAction() == OnDeleteAction.CASCADE;
	}

	@Override
	public String getExplicitForeignKeyName() {
		return getEntityClass().getExplicitForeignKeyName();
	}

	@Override
	public JoinColumnResolutionDelegate getForeignKeyTargetColumnResolutionDelegate() {
		return fkColumnResolutionDelegate;
	}

	@Override
	public List<ColumnSource> getPrimaryKeyColumnSources() {
		return columnSources;
	}

	private static class JoinColumnResolutionDelegateImpl implements JoinColumnResolutionDelegate {
		private final List<String> targetColumnNames;

		private JoinColumnResolutionDelegateImpl(List<String> targetColumnNames) {
			this.targetColumnNames = targetColumnNames;
		}

		@Override
		public List<? extends Value> getJoinColumns(JoinColumnResolutionContext context) {
			List<Value> columns = new ArrayList<Value>();
			for ( String name : targetColumnNames ) {
				// the nulls represent table, schema and catalog name which are ignored anyway...
				columns.add( context.resolveColumn( name, null, null, null ) );
			}
			return columns;
		}

		@Override
		public TableSpecification getReferencedTable(JoinColumnResolutionContext context) {
			return context.resolveTable( null, null, null );
		}

		@Override
		public String getReferencedAttributeName() {
			return null;
		}

	}



}
