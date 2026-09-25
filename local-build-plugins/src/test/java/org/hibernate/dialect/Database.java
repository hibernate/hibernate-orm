package org.hibernate.dialect;

/// Minimal automatic-resolution database fixture.
///
/// @author Steve Ebersole
public enum Database {
	SAMPLE {
		@Override
		public Dialect createDialect() {
			return new SampleDialect();
		}
	};

	public abstract Dialect createDialect();
}
