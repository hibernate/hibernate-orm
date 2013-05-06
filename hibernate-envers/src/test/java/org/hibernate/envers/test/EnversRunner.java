package org.hibernate.envers.test;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.runner.Runner;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runners.Parameterized;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.TestClass;

import org.hibernate.testing.junit4.CustomRunner;

/**
 * Copied & modified from {@link org.junit.runners.Parameterized}.
 * <p/>
 * The modification is that the generated runners extend {@link CustomRunner} instead of the default
 * {@code TestClassRunnerForParameters}.
 * <p/>
 * The runner itself sets the data using a setter instead of a constructor, and creates only one test instance. Moreover
 * it doesn't override {@code classBlock} which causes the custom {@code @BeforeClassOnce} and {@code @AfterClassOnce}
 * annotations to work.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public class EnversRunner extends Suite {
	private class TestClassCustomRunnerForParameters extends CustomRunner {
		private final int fParameterSetNumber;

		private final List<Object[]> fParameterList;

		TestClassCustomRunnerForParameters(Class<?> type, List<Object[]> parameterList, int i)
				throws InitializationError, NoTestsRemainException {
			super( type );
			fParameterList = parameterList;
			fParameterSetNumber = i;
		}

		@Override
		protected Object getTestInstance() throws Exception {
			Object testInstance = super.getTestInstance();
			if ( AbstractEnversTest.class.isInstance( testInstance ) ) {
				((AbstractEnversTest) testInstance).setTestData( computeParams() );
			}
			else if ( BaseEnversFunctionalTestCase.class.isInstance( testInstance ) ) {
				((BaseEnversFunctionalTestCase) testInstance).setTestData( computeParams() );
			}
			return testInstance;
		}

		private Object[] computeParams() throws Exception {
			try {
				return fParameterList.get( fParameterSetNumber );
			}
			catch (ClassCastException e) {
				throw new Exception(
						String.format(
								"%s.%s() must return a Collection of arrays.",
								getTestClass().getName(), getParametersMethod(
								getTestClass()
						).getName()
						)
				);
			}
		}

		@Override
		protected String getName() {
			return String.format( "[%s]", fParameterSetNumber );
		}

		@Override
		protected String testName(final FrameworkMethod method) {
			return String.format(
					"%s[%s]", method.getName(),
					fParameterSetNumber
			);
		}

		@Override
		protected void sortMethods(List<FrameworkMethod> computedTestMethods) {
			super.sortMethods( computedTestMethods );
			Collections.sort(
					computedTestMethods, new Comparator<FrameworkMethod>() {
				private int getPriority(FrameworkMethod fm) {
					Priority p = fm.getAnnotation( Priority.class );
					return p == null ? 0 : p.value();
				}

				@Override
				public int compare(FrameworkMethod fm1, FrameworkMethod fm2) {
					return getPriority( fm2 ) - getPriority( fm1 );
				}
			}
			);
		}
	}

	private final ArrayList<Runner> runners = new ArrayList<Runner>();

	/**
	 * Only called reflectively. Do not use programmatically.
	 */
	public EnversRunner(Class<?> klass) throws Throwable {
		super( klass, Collections.<Runner>emptyList() );
		List<Object[]> parametersList = getParametersList( getTestClass() );
		for ( int i = 0; i < parametersList.size(); i++ ) {
			runners.add(
					new TestClassCustomRunnerForParameters(
							getTestClass().getJavaClass(),
							parametersList, i
					)
			);
		}
	}

	@Override
	protected List<Runner> getChildren() {
		return runners;
	}

	@SuppressWarnings("unchecked")
	private List<Object[]> getParametersList(TestClass klass)
			throws Throwable {
		return (List<Object[]>) getParametersMethod( klass ).invokeExplosively(
				null
		);
	}

	private FrameworkMethod getParametersMethod(TestClass testClass)
			throws Exception {
		List<FrameworkMethod> methods = testClass.getAnnotatedMethods( Parameterized.Parameters.class );

		for ( FrameworkMethod each : methods ) {
			int modifiers = each.getMethod().getModifiers();
			if ( Modifier.isStatic( modifiers ) && Modifier.isPublic( modifiers ) ) {
				return each;
			}
		}

		throw new Exception(
				"No public static parameters method on class "
						+ testClass.getName()
		);
	}

}
