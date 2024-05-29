/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.annotations.Table;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.HibernateAnnotations.TABLE;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class TableAnnotation implements Table {
	private String appliesTo;
	private FetchMode fetch;
	private boolean inverse;
	private boolean optional;
	private String checkConstraint;
	private String comment;
	private SQLInsert sqlInsert;
	private SQLUpdate sqlUpdate;
	private SQLDelete sqlDelete;
	private ForeignKey foreignKey;
	private Index[] indexes;

	public TableAnnotation(SourceModelBuildingContext modelContext) {
		this.appliesTo = "";
		this.fetch = FetchMode.JOIN;
		this.inverse = false;
		this.optional = true;
		this.checkConstraint = "";
		this.comment = "";
		this.sqlInsert = new SQLInsertAnnotation( modelContext );
		this.sqlUpdate = new SQLUpdateAnnotation( modelContext );
		this.sqlDelete = new SQLDeleteAnnotation( modelContext );
		this.foreignKey = new ForeignKeyAnnotation( modelContext );
		this.indexes = new Index[0];
	}

	public TableAnnotation(Table annotation, SourceModelBuildingContext modelContext) {
		this.appliesTo = annotation.appliesTo();
		this.fetch = annotation.fetch();
		this.inverse = annotation.inverse();
		this.optional = annotation.optional();
		this.indexes = extractJdkValue( annotation, TABLE, "indexes", modelContext );
		this.checkConstraint = annotation.checkConstraint();
		this.comment = annotation.comment();
		this.sqlInsert = extractJdkValue( annotation, TABLE, "sqlInsert", modelContext );
		this.sqlUpdate = extractJdkValue( annotation, TABLE, "sqlUpdate", modelContext );
		this.sqlDelete = extractJdkValue( annotation, TABLE, "sqlDelete", modelContext );
		this.foreignKey = extractJdkValue( annotation, TABLE, "foreignKey", modelContext );
		this.indexes = extractJdkValue( annotation, TABLE, "indexes", modelContext );
	}

	public TableAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.appliesTo = extractJandexValue( annotation, TABLE, "appliesTo", modelContext );
		this.fetch = extractJandexValue( annotation, TABLE, "fetch", modelContext );
		this.inverse = extractJandexValue( annotation, TABLE, "inverse", modelContext );
		this.optional = extractJandexValue( annotation, TABLE, "optional", modelContext );
		this.indexes = extractJandexValue( annotation, TABLE, "indexes", modelContext );
		this.checkConstraint = extractJandexValue(
				annotation,
				TABLE,
				"checkConstraint",
				modelContext
		);
		this.comment = extractJandexValue( annotation, TABLE, "comment", modelContext );
		this.sqlInsert = extractJandexValue( annotation, TABLE, "sqlInsert", modelContext );
		this.sqlUpdate = extractJandexValue( annotation, TABLE, "sqlUpdate", modelContext );
		this.sqlDelete = extractJandexValue( annotation, TABLE, "sqlDelete", modelContext );
		this.foreignKey = extractJandexValue( annotation, TABLE, "foreignKey", modelContext );
		this.indexes = extractJandexValue( annotation, TABLE, "indexes", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Table.class;
	}

	@Override
	public String appliesTo() {
		return appliesTo;
	}

	public void appliesTo(String value) {
		this.appliesTo = value;
	}

	@Override
	public FetchMode fetch() {
		return fetch;
	}

	public void fetch(FetchMode value) {
		this.fetch = value;
	}

	@Override
	public boolean inverse() {
		return inverse;
	}

	public void inverse(boolean value) {
		this.inverse = value;
	}

	@Override
	public boolean optional() {
		return optional;
	}

	public void optional(boolean value) {
		this.optional = value;
	}

	@Override
	public String checkConstraint() {
		return checkConstraint;
	}

	public void checkConstraint(String value) {
		this.checkConstraint = value;
	}

	@Override
	public String comment() {
		return comment;
	}

	public void comment(String value) {
		this.comment = value;
	}

	@Override
	public ForeignKey foreignKey() {
		return foreignKey;
	}

	public void foreignKey(ForeignKey value) {
		this.foreignKey = value;
	}

	@Override
	public SQLInsert sqlInsert() {
		return sqlInsert;
	}

	public void sqlInsert(SQLInsert value) {
		this.sqlInsert = value;
	}

	@Override
	public SQLUpdate sqlUpdate() {
		return sqlUpdate;
	}

	public void sqlUpdate(SQLUpdate value) {
		this.sqlUpdate = value;
	}

	@Override
	public SQLDelete sqlDelete() {
		return sqlDelete;
	}

	public void sqlDelete(SQLDelete value) {
		this.sqlDelete = value;
	}

	@Override
	public Index[] indexes() {
		return indexes;
	}

	public void indexes(Index[] value) {
		this.indexes = value;
	}

}
