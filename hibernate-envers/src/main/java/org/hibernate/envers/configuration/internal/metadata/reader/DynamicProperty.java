/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.configuration.internal.metadata.reader;

import java.lang.annotation.Annotation;
import java.util.Collection;

import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;

/**
 * This class prenteds to be property but in fact it represents entry in the map (for dynamic component)
 *
 * @author Lukasz Zuchowski (author at zuchos dot com)
 */
public class DynamicProperty implements XProperty {

	private AuditedPropertiesReader.DynamicComponentSource source;
	private String propertyName;

	public DynamicProperty(AuditedPropertiesReader.DynamicComponentSource source, String propertyName) {
		this.source = source;
		this.propertyName = propertyName;
	}

	@Override
	public XClass getDeclaringClass() {
		return source.getXClass();
	}

	@Override
	public String getName() {
		return propertyName;
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	@Override
	public boolean isArray() {
		return false;
	}

	@Override
	public Class<? extends Collection> getCollectionClass() {
		return null;
	}

	@Override
	public XClass getType() {
		return source.getXClass();
	}

	@Override
	public XClass getElementClass() {
		return null;
	}

	@Override
	public XClass getClassOrElementClass() {
		return null;
	}

	@Override
	public XClass getMapKey() {
		return null;
	}

	@Override
	public int getModifiers() {
		return 0;
	}

	@Override
	public void setAccessible(boolean accessible) {
	}

	@Override
	public Object invoke(Object target, Object... parameters) {
		return null;
	}

	@Override
	public boolean isTypeResolved() {
		return false;
	}

	@Override
	public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
		return null;
	}

	@Override
	public <T extends Annotation> boolean isAnnotationPresent(Class<T> annotationType) {
		return false;
	}

	@Override
	public Annotation[] getAnnotations() {
		return new Annotation[0];
	}
}
