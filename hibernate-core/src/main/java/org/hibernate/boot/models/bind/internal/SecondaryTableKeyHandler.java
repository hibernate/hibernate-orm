/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.bind.internal;

import java.util.List;

import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.KeyValue;
import org.hibernate.models.spi.AnnotationUsage;

import jakarta.persistence.ForeignKey;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;

/**
 * Handles binding for {@linkplain PrimaryKeyJoinColumn} references for secondary and joined tables.
 *
 * @author Steve Ebersole
 */
public record SecondaryTableKeyHandler(
		AnnotationUsage<SecondaryTable> annotationUsage,
		Join join,
		EntityBinding entityBinding,
		MetadataBuildingContext buildingContext) implements ResolutionCallback<KeyValue> {

	@Override
	public boolean handleResolution(KeyValue targetKeyValue) {
		final DependantValue fkValue = new DependantValue( buildingContext(), join.getTable(), targetKeyValue );
		join.setKey( fkValue );

		final List<AnnotationUsage<PrimaryKeyJoinColumn>> pkJoinColumns =	annotationUsage.getList( "pkJoinColumns" );

		if ( CollectionHelper.isEmpty( pkJoinColumns ) ) {
			handleImplicitJoinColumns( targetKeyValue, fkValue );
		}
		else {
			throw new UnsupportedOperationException( "Not yet implemented" );
		}

		final AnnotationUsage<ForeignKey> foreignKeyAnn = annotationUsage.getAttributeValue( "foreignKey" );
		// todo : generate the name here - this *is* equiv to legacy second pass
		final String foreignKeyName = foreignKeyAnn == null
				? ""
				: foreignKeyAnn.getString( "name" );
		final String foreignKeyDefinition = foreignKeyAnn == null
				? ""
				: foreignKeyAnn.getString( "foreignKeyDefinition" );

		final RootEntityBinding rootEntityBinding = entityBinding.getRootEntityBinding();
		final org.hibernate.mapping.ForeignKey foreignKey = join.getTable().createForeignKey(
				foreignKeyName,
				targetKeyValue.getColumns(),
				rootEntityBinding.getPersistentClass().getEntityName(),
				foreignKeyDefinition,
				rootEntityBinding.getTableReference().table().getPrimaryKey().getColumns()
		);
		foreignKey.setReferencedTable( rootEntityBinding.getTableReference().table() );

		return true;
	}

	private void handleImplicitJoinColumns(KeyValue targetKeyValue, DependantValue fkValue) {
		targetKeyValue.getColumns().forEach( (column) -> {
			final Column fkColumn = column.clone();
			join.getTable().getPrimaryKey().addColumn( fkColumn );
			fkValue.addColumn( fkColumn );
		} );
	}
}
