package org.hibernate.metamodel.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.metamodel.spi.Instantiator;

/**
 * Base support for dynamic-map instantiators
 *
 * @author Steve Ebersole
 */
public abstract class AbstractDynamicMapInstantiator implements Instantiator {
	public static final String TYPE_KEY = "$type$";

	private final String roleName;

	public AbstractDynamicMapInstantiator(@Nonnull String roleName) {
		if ( roleName == null ) {
			throw new IllegalArgumentException( "Role name passed to dynamic map instantiator cannot be null" );
		}
		this.roleName = roleName;
	}

	@Nonnull
	public String getRoleName() {
		return roleName;
	}

	@Override
	public boolean isInstance(@Nullable Object object) {
		return object instanceof Map<?, ?> map
			// TODO: should this be an exception if there is no TYPE_KEY
			&& roleName.equals( map.get( TYPE_KEY ) );
	}

	protected boolean isSameRole(@Nullable String type) {
		return roleName.equals( type );
	}

	@Override
	public boolean isSameClass(@Nonnull Object object) {
		return isInstance( object );
	}

	@Nonnull
	protected Map<String,?> generateDataMap() {
		final Map<String,Object> map = new HashMap<>();
		map.put( TYPE_KEY, roleName );
		return map;
	}
}
