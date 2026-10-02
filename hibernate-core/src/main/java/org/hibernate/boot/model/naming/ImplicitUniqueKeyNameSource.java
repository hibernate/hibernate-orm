package org.hibernate.boot.model.naming;

/**
 * @author Steve Ebersole
 */
public non-sealed interface ImplicitUniqueKeyNameSource
		extends ImplicitConstraintNameSource {
	@Override
	default Kind kind() {
		return Kind.UNIQUE_KEY;
	}
}
