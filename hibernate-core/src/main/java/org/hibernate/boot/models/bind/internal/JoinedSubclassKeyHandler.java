/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.bind.internal;

import java.util.List;
import java.util.Locale;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.KeyValue;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.ClassDetails;

import jakarta.persistence.ForeignKey;
import jakarta.persistence.PrimaryKeyJoinColumn;

/**
 * @author Steve Ebersole
 */
public record JoinedSubclassKeyHandler(
		ClassDetails subclassDetails,
		JoinedSubclass subclass,
		MetadataBuildingContext buildingContext) implements ResolutionCallback<KeyValue> {

	@Override
	public boolean handleResolution(KeyValue targetKeyValue) {
		final List<AnnotationUsage<PrimaryKeyJoinColumn>> pkJoinColumns = subclassDetails.getRepeatedAnnotationUsages( PrimaryKeyJoinColumn.class );
		final DependantValue fkValue = new DependantValue( buildingContext, subclass.getTable(), targetKeyValue );

		if ( CollectionHelper.isEmpty( pkJoinColumns ) ) {
			handleImplicitJoinColumns( targetKeyValue, fkValue );
		}
		else {
			handleExplicitJoinColumns( pkJoinColumns, targetKeyValue, fkValue );
		}

		final AnnotationUsage<ForeignKey> fkAnn = subclassDetails.getAnnotationUsage( ForeignKey.class );

		final String foreignKeyName = fkAnn == null
				// todo : generate the name here - this *is* equiv to legacy second pass
				? ""
				: fkAnn.getString( "name" );
		final String foreignKeyDefinition = fkAnn == null
				? ""
				: fkAnn.getString( "foreignKeyDefinition" );

		final org.hibernate.mapping.ForeignKey foreignKey = subclass.getTable().createForeignKey(
				foreignKeyName,
				fkValue.getColumns(),
				subclass.getRootClass().getEntityName(),
				foreignKeyDefinition,
				targetKeyValue.getColumns()
		);
		foreignKey.setReferencedTable( subclass.getRootTable() );

		return true;
	}

	private void handleImplicitJoinColumns(KeyValue targetKeyValue, DependantValue fkValue) {
		targetKeyValue.getColumns().forEach( (column) -> {
			final Column fkColumn = column.clone();
			subclass.getTable().getPrimaryKey().addColumn( fkColumn );
			fkValue.addColumn( fkColumn );
		} );
	}

	private void handleExplicitJoinColumns(
			List<AnnotationUsage<PrimaryKeyJoinColumn>> pkJoinColumns,
			KeyValue targetKeyValue,
			DependantValue fkValue) {
		for ( int i = 0; i < targetKeyValue.getColumnSpan(); i++ ) {
			final Column targetColumn = targetKeyValue.getColumns().get( i );
			final var joinColumnAnn = resolveMatchingJoinColumnAnn(
					targetColumn,
					pkJoinColumns,
					i
			);

			final Column keyColumn = ColumnHelper.bindColumn( joinColumnAnn, targetColumn::getName, true, false );
			subclass().getTable().getPrimaryKey().addColumn( keyColumn );
			fkValue.addColumn( keyColumn );
		}
	}

	private AnnotationUsage<PrimaryKeyJoinColumn> resolveMatchingJoinColumnAnn(
			Column targetPkColumn,
			List<AnnotationUsage<PrimaryKeyJoinColumn>> keyJoinColumns,
			int pkColumnPosition) {
		for ( int j = 0; j < keyJoinColumns.size(); j++ ) {
			final var keyJoinColumn = keyJoinColumns.get( j );
			final String name = keyJoinColumn.getString( "name" );
			final String referencedColumnName = keyJoinColumn.getString( "referencedColumnName" );
			if ( StringHelper.isEmpty( name ) && StringHelper.isEmpty( referencedColumnName ) ) {
				// assume positional match
				// todo : is this correct?  the only other option is to throw an exception
				if ( j == pkColumnPosition ) {
					return keyJoinColumn;
				}
			}
			else if ( StringHelper.isNotEmpty( referencedColumnName ) ) {
				if ( targetPkColumn.getName().equals( referencedColumnName ) ) {
					return keyJoinColumn;
				}
			}
			else {
				assert StringHelper.isNotEmpty( name );
				if ( targetPkColumn.getName().equals( name ) ) {
					return keyJoinColumn;
				}
			}
		}

		throw new MappingException(
				String.format(
						Locale.ROOT,
						"Unable to match primary key column [%s] to any PrimaryKeyJoinColumn - %s",
						targetPkColumn.getName(),
						subclass().getEntityName()
				)
		);
	}
}
