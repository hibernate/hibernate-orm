package org.hibernate.boot.model.process.internal;

import java.util.Collections;
import java.util.LinkedHashMap;
import org.hibernate.boot.BootLogging;
import org.hibernate.models.spi.ClassDetails;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.jaxb.spi.JaxbBindableMappingDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.cfg.MappingSettings;



/// Structurally immutable snapshot of one resource batch.
///
/// @author Steve Ebersole
public final class ManagedResourcesImpl implements ManagedResources {
	private final List<ConverterDescriptor<?, ?>> attributeConverterDescriptors;
	private final List<Class<?>> annotatedClassReferences;
	private final List<String> annotatedClassNames;
	private final List<ClassDetails> classDetails;
	private final List<String> annotatedPackageNames;
	private final List<String> annotatedModuleNames;
	private final List<Binding<? extends JaxbBindableMappingDescriptor>> xmlMappingBindings;
	private final Map<String, Class<?>> extraQueryImports;

	ManagedResourcesImpl(ManagedResourcesBuilder builder) {
		attributeConverterDescriptors = List.copyOf( builder.converters.values() );
		annotatedClassReferences = List.copyOf( builder.classes.values() );
		annotatedClassNames = List.copyOf( builder.classNames );
		classDetails = List.copyOf( builder.details.values() );
		annotatedPackageNames = List.copyOf( builder.packages );
		annotatedModuleNames = List.copyOf( builder.modules );
		xmlMappingBindings = List.copyOf( builder.mappings );
		extraQueryImports = Collections.unmodifiableMap( new LinkedHashMap<>( builder.imports ) );
	}

	public static ManagedResourcesImpl baseline(MetadataSources sources, BootstrapContext context) {
		final var builder = new ManagedResourcesBuilder();
		context.getAttributeConverters().forEach( builder::addAttributeConverter );
		sources.getAnnotatedClasses().forEach( builder::addClass );
		sources.getAnnotatedClassNames().forEach( builder::addClassName );
		sources.getAnnotatedPackages().forEach( builder::addPackageDescriptor );
		sources.getAnnotatedModuleNames().forEach( builder::addModuleDescriptor );
		if ( context.getMetadataBuildingOptions().isXmlMappingEnabled() ) {
			sources.getMappingXmlBindings().forEach( builder::addXmlBinding );
			sources.getHbmXmlBindings().forEach( builder::addXmlBinding );
		}
		else {
			BootLogging.BOOT_LOGGER.ignoringXmlMappings(
					sources.getMappingXmlBindings().size(), MappingSettings.XML_MAPPING_ENABLED );
		}
		if ( sources.getExtraQueryImports() != null ) {
			sources.getExtraQueryImports().forEach( builder::addQueryImport );
		}
		return new ManagedResourcesImpl( builder );
	}

	@Override
	public Collection<ConverterDescriptor<?, ?>> getAttributeConverterDescriptors() {
		return attributeConverterDescriptors;
	}

	@Override
	public Collection<Class<?>> getAnnotatedClassReferences() {
		return annotatedClassReferences;
	}

	@Override
	public Collection<String> getAnnotatedClassNames() {
		return annotatedClassNames;
	}

	@Override
	public Collection<ClassDetails> getClassDetails() {
		return classDetails;
	}

	@Override
	public Collection<String> getAnnotatedPackageNames() {
		return annotatedPackageNames;
	}

	@Override
	public Collection<String> getAnnotatedModuleNames() {
		return annotatedModuleNames;
	}

	@Override
	public Collection<Binding<? extends JaxbBindableMappingDescriptor>> getXmlMappingBindings() {
		return xmlMappingBindings;
	}

	@Override
	public Map<String, Class<?>> getExtraQueryImports() {
		return extraQueryImports;
	}
}
