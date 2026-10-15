package org.hibernate.orm.test.instrumentation;

import jakarta.persistence.MappedSuperclass;

/// Triggers generic signature parsing during enhancement discovery.
///
/// @author Steve Ebersole
@MappedSuperclass
public abstract class GenericMappedSuperclass<T> {

	T payload;
}
