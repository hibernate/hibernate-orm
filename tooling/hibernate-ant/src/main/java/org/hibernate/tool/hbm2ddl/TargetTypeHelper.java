/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.hbm2ddl;

import java.util.EnumSet;

import org.hibernate.tool.schema.TargetType;

/**
 * @author Steve Ebersole
 */
public class TargetTypeHelper {
	public static EnumSet<TargetType> parseLegacyCommandLineOptions(boolean script, boolean export, String outputFile) {
		final EnumSet<TargetType> options = EnumSet.noneOf( TargetType.class );

		final Target target = Target.interpret( script, export );
		if ( outputFile != null ) {
			options.add( TargetType.SCRIPT );
		}
		if ( target.doScript() ) {
			options.add( TargetType.STDOUT );
		}
		if ( target.doExport() ) {
			options.add( TargetType.DATABASE );
		}
		return options;
	}

	public static EnumSet<TargetType> parseCommandLineOptions(String targetTypeText) {
		final EnumSet<TargetType> options = EnumSet.noneOf( TargetType.class );

		if ( !targetTypeText.equalsIgnoreCase( "none" ) ) {
			for ( String option : targetTypeText.split( "," ) ) {
				if ( option.equalsIgnoreCase( "database" ) ) {
					options.add( TargetType.DATABASE );
				}
				else if ( option.equalsIgnoreCase( "stdout" ) ) {
					options.add( TargetType.STDOUT );
				}
				else if ( option.equalsIgnoreCase( "script" ) ) {
					options.add( TargetType.SCRIPT );
				}
				else {
					throw new IllegalArgumentException( "Unrecognized --target option : " + option );
				}
			}
		}

		return options;
	}
}
