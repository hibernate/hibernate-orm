package org.hibernate.boot.model.process.internal;

import java.util.Objects;
import org.hibernate.MappingException;

/// Validates ordinary type inputs without loading application classes.
///
/// @author Steve Ebersole
public final class ManagedResourceValidation {
	private ManagedResourceValidation() {
	}

	public static void validateClassName(String name, String input, String packageReplacement, String moduleReplacement) {
		Objects.requireNonNull( name, "class name" );
		if ( name.equals( "package-info" ) || name.endsWith( ".package-info" ) ) {
			final String replacement = name.equals( "package-info" )
					? "an explicit valid package descriptor"
					: packageReplacement.replace( "{package}", name.substring( 0, name.length() - ".package-info".length() ) );
			throw new MappingException( input.replace( "{class}", name ) + " is illegal, use " + replacement + " instead" );
		}
		if ( name.equals( "module-info" ) || name.endsWith( ".module-info" ) ) {
			throw new MappingException( input.replace( "{class}", name ) + " is illegal, use " + moduleReplacement + " with the declared module name instead" );
		}
	}

	public static void validateClassName(String name) {
		validateClassName( name, "Class '{class}'", "addPackageDescriptor(\"{package}\")", "addModuleDescriptor()" );
	}
}
