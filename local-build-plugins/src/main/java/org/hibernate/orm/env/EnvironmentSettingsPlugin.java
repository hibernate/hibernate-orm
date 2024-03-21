package org.hibernate.orm.env;

import java.util.Map;
import java.util.Objects;

import org.gradle.StartParameter;
import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;

/**
 * @author Steve Ebersole
 */
public class EnvironmentSettingsPlugin implements Plugin<Settings> {

	@Override
	public void apply(Settings settings) {
		settings.getExtensions().add( JpaVersion.EXT_KEY, JpaVersion.from( settings ) );
		settings.getExtensions().add( "db", Objects.requireNonNullElse( getP( settings, "db" ), "h2" ) );
		String ciNode = getP( settings, "ci.node" );
		if ( ciNode != null ) {
			settings.getExtensions().add( "ci.node", ciNode );
		}
	}

	private static String getP(Settings settings, String key) {
		StartParameter startParameter = settings.getStartParameter();
		// the `-P` settings passed at command-line
		final Map<String, String> projectProperties = startParameter.getProjectProperties();
		final String projectProperty = projectProperties.get( key );
		if ( projectProperty != null && !projectProperty.isEmpty() ) {
			return projectProperty;
		}
		else {
			return null;
		}
	}

}
