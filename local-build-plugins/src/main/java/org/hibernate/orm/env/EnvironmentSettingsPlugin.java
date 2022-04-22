package org.hibernate.orm.env;

import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;

/**
 * @author Steve Ebersole
 */
public class EnvironmentSettingsPlugin implements Plugin<Settings> {

	@Override
	public void apply(Settings settings) {
		settings.getExtensions().add( JpaVersion.EXT_KEY, JpaVersion.from( settings ) );
	}
}
