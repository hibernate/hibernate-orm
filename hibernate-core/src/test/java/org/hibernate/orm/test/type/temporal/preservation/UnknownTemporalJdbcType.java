package org.hibernate.orm.test.type.temporal.preservation;

/// An unrecognized subclass must not inherit another pair's preservation claims.
///
/// @author Steve Ebersole
public class UnknownTemporalJdbcType extends org.hibernate.type.descriptor.jdbc.TimestampJdbcType {
}
