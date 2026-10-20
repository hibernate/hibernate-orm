package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.Changelog;
import org.hibernate.models.spi.ModelsContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ChangelogModifiedEntitiesAnnotation implements Changelog.ModifiedEntities {

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public ChangelogModifiedEntitiesAnnotation(ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public ChangelogModifiedEntitiesAnnotation(Changelog.ModifiedEntities annotation, ModelsContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public ChangelogModifiedEntitiesAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Changelog.ModifiedEntities.class;
	}
}
