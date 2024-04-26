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

import static org.hibernate.boot.models.internal.AnnotationUsageHelper.applyAttributeIfSpecified;
import static org.hibernate.boot.models.internal.AnnotationUsageHelper.applyStringAttributeIfSpecified;


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
		final MutableAnnotationUsage<JoinColumn> joinColumn = JpaAnnotations.JOIN_COLUMN.createUsage( context.getMetadataCollector().getSourceModelBuildingContext() );

		applyStringAttributeIfSpecified(
				"name",
				mapKeyJoinColumn.getAttributeValue( "name" ),
				joinColumn
		);
		applyStringAttributeIfSpecified(
				"table",
				mapKeyJoinColumn.getAttributeValue( "table" ),
				joinColumn
		);
		applyAttributeIfSpecified(
				"unique",
				mapKeyJoinColumn.getAttributeValue( "unique" ),
				joinColumn
		);
		applyAttributeIfSpecified(
				"nullable",
				mapKeyJoinColumn.getAttributeValue( "nullable" ),
				joinColumn
		);
		applyAttributeIfSpecified(
				"insertable",
				mapKeyJoinColumn.getAttributeValue( "insertable" ),
				joinColumn
		);
		applyStringAttributeIfSpecified(
				"referencedColumnName",
				mapKeyJoinColumn.getAttributeValue( "referencedColumnName" ),
				joinColumn
		);
		applyStringAttributeIfSpecified(
				"columnDefinition",
				mapKeyJoinColumn.getAttributeValue( "columnDefinition" ),
				joinColumn
		);
		applyStringAttributeIfSpecified(
				"options",
				mapKeyJoinColumn.getAttributeValue( "options" ),
				joinColumn
		);
//		joinColumn.setAttributeValue( "comment", mapKeyJoinColumn.getAttributeValue( "comment" ) );
		applyAttributeIfSpecified(
				"foreignKey",
				mapKeyJoinColumn.getAttributeValue( "foreignKey" ),
				joinColumn
		);

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
