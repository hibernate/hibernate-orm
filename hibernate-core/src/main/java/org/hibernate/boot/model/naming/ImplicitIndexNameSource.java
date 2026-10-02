package org.hibernate.boot.model.naming;

/**
 * @author Steve Ebersole
 */
public non-sealed interface ImplicitIndexNameSource
		extends ImplicitConstraintNameSource {
	@Override
	default Kind kind() {
		return Kind.INDEX;
	}
}
