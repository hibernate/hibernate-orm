/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.process;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.jaxb.spi.JaxbBindableMappingDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.process.spi.ManagedResources;

/**
 * @author Steve Ebersole
 */
public class ManagedResourcesImpl implements ManagedResources {
	private final Collection<Class<?>> knownClasses;
	private final Collection<String> packageNames;
	private final Collection<Binding<JaxbBindableMappingDescriptor>> xmlMappings;

	public ManagedResourcesImpl(
			Collection<Class<?>> knownClasses,
			Collection<String> packageNames,
			Collection<Binding<JaxbBindableMappingDescriptor>> xmlMappings) {
		this.knownClasses = knownClasses;
		this.packageNames = packageNames;
		this.xmlMappings = xmlMappings;
	}

	@Override
	public Collection<ConverterDescriptor> getAttributeConverterDescriptors() {
		return Collections.emptyList();
	}

	@Override
	public Collection<Class<?>> getAnnotatedClassReferences() {
		return knownClasses == null ? Collections.emptyList() : knownClasses;
	}

	@Override
	public Collection<String> getAnnotatedClassNames() {
		return Collections.emptyList();
	}

	@Override
	public Collection<String> getAnnotatedPackageNames() {
		return packageNames == null ? Collections.emptyList() : packageNames;
	}

	@Override
	public Collection<Binding<JaxbBindableMappingDescriptor>> getXmlMappingBindings() {
		if ( xmlMappings == null ) {
			return Collections.emptyList();
		}

		//noinspection unchecked,rawtypes
		return (Collection) xmlMappings;
	}

	@Override
	public Map<String, Class<?>> getExtraQueryImports() {
		return Collections.emptyMap();
	}

	public static class Builder {
		private final MappingBinder mappingBinder;

		private List<Class<?>> classes;
		private List<String> packageNames;
		private Collection<Binding<JaxbBindableMappingDescriptor>> xmlMappings;

		public Builder() {
			mappingBinder = new MappingBinder(
					(resourceName) -> Builder.class.getClassLoader().getResourceAsStream( resourceName ),
					new MappingBinder.Options() {
						@Override
						public boolean validateMappings() {
							return true;
						}

						@Override
						public boolean transformHbmMappings() {
							return false;
						}
					}
			);
		}

		public Builder addLoadedClasses(Class<?>... classes) {
			if ( this.classes == null ) {
				this.classes = new ArrayList<>();
			}
			Collections.addAll( this.classes, classes );
			return this;
		}

		public Builder addPackages(String... packageNames) {
			if ( this.packageNames == null ) {
				this.packageNames = new ArrayList<>();
			}
			Collections.addAll( this.packageNames, packageNames );
			return this;
		}

		public ManagedResources build() {
			return new ManagedResourcesImpl( classes, packageNames, xmlMappings );
		}

		public Builder addXmlMappings(String resourceName) {
			final Binding<JaxbBindableMappingDescriptor> binding = mappingBinder.bind(
					Builder.class.getClassLoader().getResourceAsStream( resourceName ),
					new Origin( SourceType.RESOURCE, resourceName )
			);
			if ( xmlMappings == null ) {
				xmlMappings = new ArrayList<>();
			}
			xmlMappings.add( binding );
			return this;
		}
	}
}
