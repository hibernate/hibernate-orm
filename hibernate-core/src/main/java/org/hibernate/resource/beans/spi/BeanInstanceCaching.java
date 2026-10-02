package org.hibernate.resource.beans.spi;

import org.hibernate.SPI;
import org.hibernate.Incubating;

import static org.hibernate.SPI.Role.USE;

/// Controls caching of bean references obtained from the [ManagedBeanRegistry].
/// This policy does not change the underlying bean container's scope semantics.
///
/// @since 8.0
/// @author Sean Okafor
/// @author Steve Ebersole
@Incubating(since = "8.0", group = "bootstrap-safe-beans")
@SPI(USE)
public enum BeanInstanceCaching {
	/// Allow reuse of a shared, cached instance. Repeated calls for the
	/// same bean class _may_ return the same [ManagedBean] handle.
	ALLOW,

	/// Bypass Hibernate's bean cache for both lookup and storage. Each call
	/// returns an independently managed [ManagedBean] handle that must be
	/// released via [ManagedBeanRegistry#releaseBean]. The underlying container's
	/// scope semantics may still cause handles to refer to the same bean instance.
	DISALLOW
}
