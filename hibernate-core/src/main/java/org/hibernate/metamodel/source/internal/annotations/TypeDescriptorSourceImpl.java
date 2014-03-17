/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.internal.annotations;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.AnnotationException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.source.internal.annotations.util.JandexHelper;
import org.hibernate.metamodel.source.spi.TypeDescriptorSource;

import org.jboss.jandex.AnnotationInstance;

/**
 * @author Steve Ebersole
 */
public class TypeDescriptorSourceImpl implements TypeDescriptorSource {
	private final String name;
	private final String implementationClassName;
	private final String[] registrationKeys;
	private final AnnotationBindingContext bindingContext;

	private Map<String, String> parameterValueMap;

	public TypeDescriptorSourceImpl(AnnotationInstance typeDefAnnotation, AnnotationBindingContext bindingContext) {
		this.bindingContext = bindingContext;
		this.name = JandexHelper.getValue(
				typeDefAnnotation, "name", String.class,
				bindingContext.getBuildingOptions().getServiceRegistry().getService( ClassLoaderService.class )
		);
		this.implementationClassName = JandexHelper.getValue( typeDefAnnotation, "typeClass", String.class,
				bindingContext.getBuildingOptions().getServiceRegistry().getService( ClassLoaderService.class ) );

		String defaultForType = JandexHelper.getValue( typeDefAnnotation, "defaultForType", String.class,
				bindingContext.getBuildingOptions().getServiceRegistry().getService( ClassLoaderService.class ) );
		if ( defaultForType != null ) {
			if ( void.class.getName().equals( defaultForType ) ) {
				defaultForType = null;
			}
		}
		String registrationKey = defaultForType;

		if ( StringHelper.isEmpty( name ) && registrationKey == null ) {
			throw new AnnotationException(
					String.format(
							"Either name or defaultForType (or both) must be set on TypeDefinition [%s]",
							implementationClassName
					)
			);
		}

		this.registrationKeys = registrationKey == null ? new String[0] : new String[] { registrationKey };
		this.parameterValueMap = extractParameterValues( typeDefAnnotation );
	}

	private Map<String, String> extractParameterValues(AnnotationInstance typeDefAnnotation) {
		Map<String, String> parameterMaps = new HashMap<String, String>();
		AnnotationInstance[] parameterAnnotations = JandexHelper.getValue(
				typeDefAnnotation,
				"parameters",
				AnnotationInstance[].class,
				bindingContext.getBuildingOptions().getServiceRegistry().getService( ClassLoaderService.class )
		);
		for ( AnnotationInstance parameterAnnotation : parameterAnnotations ) {
			parameterMaps.put(
					JandexHelper.getValue( parameterAnnotation, "name", String.class,
							bindingContext.getBuildingOptions().getServiceRegistry().getService( ClassLoaderService.class ) ),
					JandexHelper.getValue( parameterAnnotation, "value", String.class,
							bindingContext.getBuildingOptions().getServiceRegistry().getService( ClassLoaderService.class ) )
			);
		}
		return parameterMaps;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getTypeImplementationClassName() {
		return implementationClassName;
	}

	@Override
	public String[] getRegistrationKeys() {
		return registrationKeys;
	}

	@Override
	public Map<String, String> getParameters() {
		return parameterValueMap;
	}
}
