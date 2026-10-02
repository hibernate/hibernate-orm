package org.hibernate.boot.model.process.internal;

import jakarta.persistence.spi.PersistenceUnitInfo;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.model.process.spi.ManagedResources;

/// Collects transformation candidates without resolving the source model.
///
/// @author Steve Ebersole
public final class EnhancementCandidates {
	private EnhancementCandidates() {
	}

	public static List<String> forContainer(PersistenceUnitInfo unit) {
		unit.getManagedClassNames().forEach( name -> ManagedResourceValidation.validateClassName(
				name, "getManagedClassNames() entry '{class}'", "getManagedPackageDescriptors() returning \"{package}\"", "getManagedModuleDescriptors()" ) );
		return forContainer( unit.getAllClassNames() );
	}

	public static List<String> forContainer(List<String> names) {
		final var result = new LinkedHashSet<String>();
		for ( var name : names ) {
			ManagedResourceValidation.validateClassName( name, "getAllClassNames() entry '{class}'",
					"getAllPackageDescriptors() returning \"{package}\"", "getAllModuleDescriptors()" );
			result.add( name );
		}
		return List.copyOf( result );
	}

	/// Values identify legacy HBM candidates, whose discovery errors are logged.
	public static Map<String, Boolean> forResources(ManagedResources resources) {
		final var result = new LinkedHashMap<String, Boolean>();
		for ( var binding : resources.getXmlMappingBindings() ) {
			if ( binding.getRoot() instanceof JaxbHbmHibernateMapping mapping ) {
				for ( var entity : mapping.getClazz() ) {
					final var name = entity.getName();
					if ( name != null ) {
						final var qualified = name.contains( "." ) || mapping.getPackage() == null || mapping.getPackage().isEmpty()
								? name : mapping.getPackage() + '.' + name;
						ManagedResourceValidation.validateClassName( qualified );
						result.putIfAbsent( qualified, true );
					}
				}
			}
		}
		resources.getAnnotatedClassNames().forEach( name -> {
			ManagedResourceValidation.validateClassName( name );
			result.put( name, false );
		} );
		resources.getAnnotatedClassReferences().forEach( type -> {
			ManagedResourceValidation.validateClassName( type.getName() );
			result.put( type.getName(), false );
		} );
		return Collections.unmodifiableMap( result );
	}
}
