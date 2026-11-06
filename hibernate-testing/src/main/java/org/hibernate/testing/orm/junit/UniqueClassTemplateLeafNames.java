package org.hibernate.testing.orm.junit;

import java.lang.reflect.Field;

import org.junit.jupiter.api.extension.BeforeClassTemplateInvocationCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;

/**
 * JUnit extension that prefixes each leaf test's display name with the
 * class-template invocation name.
 * <p>
 * Gradle flattens JUnit's test descriptor hierarchy, dropping intermediate
 * containers such as {@code @ParameterizedClass} invocations. This makes
 * all invocations of the same test method share a single Gradle test identity,
 * causing Develocity to misreport consistently-failing parameter sets as flaky
 * tests. See <a href="https://github.com/gradle/gradle/issues/39079">gradle#39079</a>.
 * <p>
 * This extension runs before each class-template invocation's children are
 * registered as dynamic tests. It reflectively modifies each child test
 * descriptor's display name to include the invocation display name as a
 * prefix, making each (invocation, method) combination report a distinct
 * test identity.
 */
public class UniqueClassTemplateLeafNames implements BeforeClassTemplateInvocationCallback {

	private static final Field DISPLAY_NAME_FIELD;

	static {
		try {
			DISPLAY_NAME_FIELD = AbstractTestDescriptor.class.getDeclaredField( "displayName" );
			DISPLAY_NAME_FIELD.setAccessible( true );
		}
		catch (NoSuchFieldException e) {
			throw new ExceptionInInitializerError( e );
		}
	}

	@Override
	public void beforeClassTemplateInvocation(ExtensionContext context) throws Exception {
		final String invocationDisplayName = context.getDisplayName();
		final TestDescriptor invocationDescriptor = getTestDescriptor( context );
		if ( invocationDescriptor == null ) {
			return;
		}
		prefixLeafDisplayNames( invocationDescriptor, invocationDisplayName + ": " );
	}

	private static void prefixLeafDisplayNames(TestDescriptor parent, String prefix) throws Exception {
		for ( TestDescriptor child : parent.getChildren() ) {
			if ( child.getType().isTest() ) {
				final String original = child.getDisplayName();
				DISPLAY_NAME_FIELD.set( child, prefix + original );
			}
			else if ( child.getType().isContainer() ) {
				prefixLeafDisplayNames( child, prefix );
			}
		}
	}

	private static TestDescriptor getTestDescriptor(ExtensionContext context) {
		Class<?> clazz = context.getClass();
		while ( clazz != null ) {
			try {
				Field field = clazz.getDeclaredField( "testDescriptor" );
				field.setAccessible( true );
				return (TestDescriptor) field.get( context );
			}
			catch (NoSuchFieldException e) {
				clazz = clazz.getSuperclass();
			}
			catch (IllegalAccessException e) {
				return null;
			}
		}
		return null;
	}
}
