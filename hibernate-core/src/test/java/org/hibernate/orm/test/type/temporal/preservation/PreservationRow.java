package org.hibernate.orm.test.type.temporal.preservation;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/// Typed entity fixture; writer identity proves cross-process row provenance.
///
/// @author Steve Ebersole
@MappedSuperclass
public abstract class PreservationRow<T> {
	@Id int id;
	long writerProcess;
	String writerJvm;
	abstract T value();
	abstract void value(T value);
}
