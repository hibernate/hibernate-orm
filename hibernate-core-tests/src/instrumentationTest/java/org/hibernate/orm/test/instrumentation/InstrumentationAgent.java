package org.hibernate.orm.test.instrumentation;

import java.lang.instrument.Instrumentation;

/// Supplies JVM instrumentation without dynamically attaching an agent.
///
/// @author Steve Ebersole
public final class InstrumentationAgent {

	private static Instrumentation instrumentation;

	public static void premain(String arguments, Instrumentation agentInstrumentation) {
		instrumentation = agentInstrumentation;
	}

	public static Instrumentation getInstrumentation() {
		return instrumentation;
	}
}
