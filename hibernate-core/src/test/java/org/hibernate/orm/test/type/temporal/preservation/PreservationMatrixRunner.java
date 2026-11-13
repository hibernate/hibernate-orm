package org.hibernate.orm.test.type.temporal.preservation;

/// Standalone JVM entry point retains committed rows across writer/readers,
/// bypassing the JUnit launcher's intentional schema cleanup.
///
/// @author Steve Ebersole
public final class PreservationMatrixRunner {
	public static void main(String[] args) throws Exception {
		for ( var suite : new AbstractPreservationTests<?>[] { new LocalDatePreservationTests(), new LocalTimePreservationTests(),
				new LocalDateTimePreservationTests(), new InstantPreservationTests(), new OffsetTimePreservationTests(),
				new OffsetDateTimePreservationTests(), new ZonedDateTimePreservationTests() } ) {
			if ( args.length == 0 || suite.domain.getSimpleName().equals( args[0] ) ) {
				suite.preservationMatrix();
			}
		}
	}
}
