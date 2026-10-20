package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.Audited;
import org.hibernate.models.spi.ModelsContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class AuditedTableAnnotation implements Audited.Table {
	private String name;
	private String schema;
	private String catalog;
	private String changesetIdColumn;
	private String modificationTypeColumn;
	private String invalidatingChangesetIdColumn;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public AuditedTableAnnotation(ModelsContext modelContext) {
		this.name = "";
		this.schema = "";
		this.catalog = "";
		this.changesetIdColumn = DEFAULT_CHANGESET_ID_COLUMN_NAME;
		this.modificationTypeColumn = DEFAULT_MODIFICATION_TYPE_COLUMN_NAME;
		this.invalidatingChangesetIdColumn = DEFAULT_INVALIDATING_CHANGESET_ID_COLUMN_NAME;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public AuditedTableAnnotation(Audited.Table annotation, ModelsContext modelContext) {
		this.name = annotation.name();
		this.schema = annotation.schema();
		this.catalog = annotation.catalog();
		this.changesetIdColumn = annotation.changesetIdColumn();
		this.modificationTypeColumn = annotation.modificationTypeColumn();
		this.invalidatingChangesetIdColumn = annotation.invalidatingChangesetIdColumn();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public AuditedTableAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.name = (String) attributeValues.get( "name" );
		this.schema = (String) attributeValues.get( "schema" );
		this.catalog = (String) attributeValues.get( "catalog" );
		this.changesetIdColumn = (String) attributeValues.get( "changesetIdColumn" );
		this.modificationTypeColumn = (String) attributeValues.get( "modificationTypeColumn" );
		this.invalidatingChangesetIdColumn = (String) attributeValues.get( "invalidatingChangesetIdColumn" );
		if ( this.name == null ) {
			this.name = "";
		}
		if ( this.schema == null ) {
			this.schema = "";
		}
		if ( this.catalog == null ) {
			this.catalog = "";
		}
		if ( this.changesetIdColumn == null ) {
			this.changesetIdColumn = DEFAULT_CHANGESET_ID_COLUMN_NAME;
		}
		if ( this.modificationTypeColumn == null ) {
			this.modificationTypeColumn = DEFAULT_MODIFICATION_TYPE_COLUMN_NAME;
		}
		if ( this.invalidatingChangesetIdColumn == null ) {
			this.invalidatingChangesetIdColumn = DEFAULT_INVALIDATING_CHANGESET_ID_COLUMN_NAME;
		}
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Audited.Table.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}

	@Override
	public String schema() {
		return schema;
	}

	public void schema(String value) {
		this.schema = value;
	}

	@Override
	public String catalog() {
		return catalog;
	}

	public void catalog(String value) {
		this.catalog = value;
	}

	@Override
	public String changesetIdColumn() {
		return changesetIdColumn;
	}

	public void changesetIdColumn(String value) {
		this.changesetIdColumn = value;
	}

	@Override
	public String modificationTypeColumn() {
		return modificationTypeColumn;
	}

	public void modificationTypeColumn(String value) {
		this.modificationTypeColumn = value;
	}

	@Override
	public String invalidatingChangesetIdColumn() {
		return invalidatingChangesetIdColumn;
	}

	public void invalidatingChangesetIdColumn(String value) {
		this.invalidatingChangesetIdColumn = value;
	}
}
