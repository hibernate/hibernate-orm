package org.hibernate.dialect;

/// Core Dialect subclass fixture.
///
/// @author Steve Ebersole
public class SampleDialect extends Dialect {
	@Override
	public String translate(String value) {
		return "sample:" + super.translate( value );
	}
}
