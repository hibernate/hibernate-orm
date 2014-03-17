/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.internal.annotations.attribute;

import org.hibernate.AssertionFailure;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.compare.EqualsHelper;
import org.hibernate.metamodel.source.internal.annotations.entity.EntityBindingContext;
import org.hibernate.metamodel.source.internal.annotations.util.JandexHelper;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public abstract class AbstractOverrideDefinition {

	protected static final String PROPERTY_PATH_SEPARATOR = ".";
	protected final String attributePath;
	protected final EntityBindingContext bindingContext;

	private boolean isApplied;

	public AbstractOverrideDefinition(String prefix, AnnotationInstance attributeOverrideAnnotation,
			EntityBindingContext bindingContext) {
		if ( attributeOverrideAnnotation == null ) {
			throw new IllegalArgumentException( "AnnotationInstance passed cannot be null" );
		}

		if ( !getTargetAnnotation().equals( attributeOverrideAnnotation.name() ) ) {
			throw new AssertionFailure( "Unexpected annotation passed to the constructor" );
		}

		this.attributePath = createAttributePath(
				prefix,
				JandexHelper.getValue(
						attributeOverrideAnnotation,
						"name",
						String.class,
						bindingContext.getBuildingOptions().getServiceRegistry().getService( ClassLoaderService.class )
				)
		);
		this.bindingContext = bindingContext;
	}

	protected static String createAttributePath(String prefix, String name) {
		if ( StringHelper.isEmpty( name ) ) {
			throw new AssertionFailure( "name attribute in @AttributeOverride can't be empty" );
		}
		String path = "";
		if ( StringHelper.isNotEmpty( prefix ) ) {
			path += prefix;
		}
		if ( StringHelper.isNotEmpty( path ) && !path.endsWith( PROPERTY_PATH_SEPARATOR ) ) {
			path += PROPERTY_PATH_SEPARATOR;
		}
		path += name;
		return path;
	}

	public String getAttributePath(){
		return attributePath;
	}

	public abstract void apply(AbstractPersistentAttribute persistentAttribute);

	protected abstract DotName getTargetAnnotation();

	public boolean isApplied() {
		return isApplied;
	}

	public void setApplied(boolean applied) {
		isApplied = applied;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !( o instanceof AbstractOverrideDefinition ) ) {
			return false;
		}

		AbstractOverrideDefinition that = (AbstractOverrideDefinition) o;
		return EqualsHelper.equals( this.attributePath, that.attributePath );
	}

	@Override
	public int hashCode() {
		return attributePath != null ? attributePath.hashCode() : 0;
	}

	@Override
	public String toString() {
		return "AbstractOverrideDefinition{attributePath='" + attributePath + "'}";
	}
}
