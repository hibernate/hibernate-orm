/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import java.lang.annotation.Annotation;

import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.MutableAnnotationUsage;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyJoinColumn;

/**
 * @author Emmanuel Bernard
 */
@SuppressWarnings({ "ClassExplicitlyAnnotation" })
public class MapKeyJoinColumnDelegator implements JoinColumn {
	private final MapKeyJoinColumn column;

	public MapKeyJoinColumnDelegator(AnnotationUsage<MapKeyJoinColumn> column) {
		this( column.toAnnotation() );
	}

	public MapKeyJoinColumnDelegator(MapKeyJoinColumn column) {
		this.column = column;
	}

	public static MutableAnnotationUsage<JoinColumn> fromMapKeyJoinColumn(
			AnnotationUsage<MapKeyJoinColumn> mapKeyJoinColumn,
			MemberDetails attributeMember,
			MetadataBuildingContext context) {
		final MutableAnnotationUsage<JoinColumn> joinColumn = JpaAnnotations.JOIN_COLUMN.createUsage(
				attributeMember,
				context.getMetadataCollector().getSourceModelBuildingContext()
		);

		joinColumn.setAttributeValue( "name", mapKeyJoinColumn.getAttributeValue( "name" ) );
		joinColumn.setAttributeValue( "table", mapKeyJoinColumn.getAttributeValue( "table" ) );
		joinColumn.setAttributeValue( "unique", mapKeyJoinColumn.getAttributeValue( "unique" ) );
		joinColumn.setAttributeValue( "nullable", mapKeyJoinColumn.getAttributeValue( "nullable" ) );
		joinColumn.setAttributeValue( "insertable", mapKeyJoinColumn.getAttributeValue( "insertable" ) );
		joinColumn.setAttributeValue( "referencedColumnName", mapKeyJoinColumn.getAttributeValue( "referencedColumnName" ) );
		joinColumn.setAttributeValue( "columnDefinition", mapKeyJoinColumn.getAttributeValue( "columnDefinition" ) );
		joinColumn.setAttributeValue( "options", mapKeyJoinColumn.getAttributeValue( "options" ) );
//		joinColumn.setAttributeValue( "comment", mapKeyJoinColumn.getAttributeValue( "comment" ) );
		joinColumn.setAttributeValue( "foreignKey", mapKeyJoinColumn.getAttributeValue( "foreignKey" ) );

		return joinColumn;
	}

	@Override
	public String name() {
		return column.name();
	}

	@Override
	public String referencedColumnName() {
		return column.referencedColumnName();
	}

	@Override
	public boolean unique() {
		return column.unique();
	}

	@Override
	public boolean nullable() {
		return column.nullable();
	}

	@Override
	public boolean insertable() {
		return column.insertable();
	}

	@Override
	public boolean updatable() {
		return column.updatable();
	}

	@Override
	public String columnDefinition() {
		return column.columnDefinition();
	}

	@Override
	public String options() {
		return column.options();
	}

	@Override
	public String table() {
		return column.table();
	}

	@Override
	public ForeignKey foreignKey() {
		return column.foreignKey();
	}

	@Override
	public CheckConstraint[] check() {
		return new CheckConstraint[0];
	}

	@Override
	public String comment() {
		return "";
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Column.class;
	}
}
