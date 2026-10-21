package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.Changelog;
import org.hibernate.audit.ChangesetListener;
import org.hibernate.models.spi.ModelsContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ChangelogAnnotation implements Changelog {
	private Class<? extends ChangesetListener> listener;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public ChangelogAnnotation(ModelsContext modelContext) {
		this.listener = ChangesetListener.class;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public ChangelogAnnotation(Changelog annotation, ModelsContext modelContext) {
		this.listener = annotation.listener();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	@SuppressWarnings("unchecked")
	public ChangelogAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.listener = (Class<? extends ChangesetListener>) attributeValues.get( "listener" );
		if ( this.listener == null ) {
			this.listener = ChangesetListener.class;
		}
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Changelog.class;
	}

	@Override
	public Class<? extends ChangesetListener> listener() {
		return listener;
	}

	public void setListener(Class<? extends ChangesetListener> listener) {
		this.listener = listener;
	}
}
