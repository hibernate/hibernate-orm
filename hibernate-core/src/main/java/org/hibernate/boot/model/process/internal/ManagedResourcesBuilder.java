package org.hibernate.boot.model.process.internal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hibernate.MappingException;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.jaxb.spi.JaxbBindableMappingDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.models.spi.ClassDetails;

import static java.util.Objects.requireNonNull;

/// Collects a batch of resources without resolving names or opening mapping files.
///
/// @author Steve Ebersole
public class ManagedResourcesBuilder {
	final Map<String, Class<?>> classes = new LinkedHashMap<>();
	final Set<String> classNames = new LinkedHashSet<>();
	final Map<String, ClassDetails> details = new LinkedHashMap<>();
	final Set<String> packages = new LinkedHashSet<>();
	final Set<String> modules = new LinkedHashSet<>();
	final List<Binding<? extends JaxbBindableMappingDescriptor>> mappings = new ArrayList<>();
	final Map<Class<?>, ConverterDescriptor<?, ?>> converters = new LinkedHashMap<>();
	final Map<String, Class<?>> imports = new LinkedHashMap<>();

	public ManagedResourcesBuilder addClass(Class<?> type) {
		requireNonNull( type );
		ManagedResourceValidation.validateClassName( type.getName() );
		final var previous = classes.putIfAbsent( type.getName(), type );
		if ( previous != null && previous != type ) {
			throw new MappingException( "Conflicting classes named '" + type.getName() + "' from class loaders "
					+ previous.getClassLoader() + " and " + type.getClassLoader() );
		}
		return this;
	}

	public ManagedResourcesBuilder addClassName(String name) {
		ManagedResourceValidation.validateClassName( name );
		classNames.add( name );
		return this;
	}

	public ManagedResourcesBuilder addClassDetails(ClassDetails value) {
		requireNonNull( value );
		ManagedResourceValidation.validateClassName( value.getName() );
		final var previous = details.putIfAbsent( value.getName(), value );
		if ( previous != null && previous != value ) {
			throw new MappingException( "Conflicting ClassDetails for '" + value.getName() + "'" );
		}
		return this;
	}

	public ManagedResourcesBuilder addPackageDescriptor(String name) {
		packages.add( requireNonNull( name ) );
		return this;
	}

	public ManagedResourcesBuilder addModuleDescriptor(String name) {
		modules.add( requireNonNull( name ) );
		return this;
	}

	public ManagedResourcesBuilder addXmlBinding(Binding<? extends JaxbBindableMappingDescriptor> binding) {
		mappings.add( requireNonNull( binding ) );
		return this;
	}

	public ManagedResourcesBuilder addAttributeConverter(ConverterDescriptor<?, ?> descriptor) {
		converters.put( requireNonNull( descriptor ).getAttributeConverterClass(), descriptor );
		return this;
	}

	public ManagedResourcesBuilder addQueryImport(String name, Class<?> type) {
		imports.put( requireNonNull( name ), requireNonNull( type ) );
		return this;
	}

	public ManagedResourcesBuilder addResources(ManagedResources resources) {
		addNonXmlResources( resources );
		resources.getXmlMappingBindings().forEach( this::addXmlBinding );
		return this;
	}

	/// Copy retained handles and registrations when replacing an XML batch.
	public ManagedResourcesBuilder addNonXmlResources(ManagedResources resources) {
		resources.getAnnotatedClassReferences().forEach( this::addClass );
		resources.getAnnotatedClassNames().forEach( this::addClassName );
		resources.getClassDetails().forEach( this::addClassDetails );
		resources.getAnnotatedPackageNames().forEach( this::addPackageDescriptor );
		resources.getAnnotatedModuleNames().forEach( this::addModuleDescriptor );
		resources.getAttributeConverterDescriptors().forEach( this::addAttributeConverter );
		if ( resources.getExtraQueryImports() != null ) {
			resources.getExtraQueryImports().forEach( this::addQueryImport );
		}
		return this;
	}

	public ManagedResources build() {
		return new ManagedResourcesImpl( this );
	}
}
