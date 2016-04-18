package org.hibernate.metamodel.internal;

/**
 * @author Steve Ebersole
 */
enum JpaMetaModelPopulationSetting {
	ENABLED,
	DISABLED,
	IGNORE_UNSUPPORTED;

	private static JpaMetaModelPopulationSetting parse(String setting) {
		if ( "enabled".equalsIgnoreCase( setting ) ) {
			return ENABLED;
		}
		else if ( "disabled".equalsIgnoreCase( setting ) ) {
			return DISABLED;
		}
		else {
			return IGNORE_UNSUPPORTED;
		}
	}
}
