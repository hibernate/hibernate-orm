/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.junit4;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.runner.Runner;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Parameterized;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkField;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

/**
 * Allows the {@link CustomRunner} features in parameterized tests.
 * This is mostly copy-paste from {@link Parameterized} since the methods could not be overridden.
 *
 * The static {@link org.junit.BeforeClass} and {@link org.junit.AfterClass} methods will be executed
 * only once beforeQuery and afterQuery all tests (since these should prepare static members).
 * Hibernate-specific {@link org.hibernate.testing.BeforeClassOnce} and {@link org.hibernate.testing.AfterClassOnce}
 * will be executed beforeQuery and afterQuery each set of tests with given parameters.
 *
 * Class can override the parameters list (annotated by {@link org.junit.runners.Parameterized.Parameters}
 * by defining static method of the same name in inheriting class (this works although usually static
 * methods cannot override each other in Java).
 *
 * When there are multiple methods providing the parameters list, the used parameters list is a cross product
 * of all the options, concatenating the argument list according to {@link Order} values.
 *
 * Contrary to {@link Parameterized}, non-static parameters methods are allowed, but the test class needs
 * to have parameterless constructor, and therefore use {@link org.junit.runners.Parameterized.Parameter}
 * for setting these parameters. This allow type-safe overriding of the method; note that only the base
 * method needs the {@link org.junit.runners.Parameterized.Parameters} annotation, overriding methods
 * are invoked automatically.
 *
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class CustomParameterized extends Suite {

	private static final List<Runner> NO_RUNNERS = Collections.emptyList();

	private final ArrayList<Runner> runners = new ArrayList<Runner>();

	/**
	 * Only called reflectively. Do not use programmatically.
	 */
	public CustomParameterized(Class<?> klass) throws Throwable {
		super(klass, NO_RUNNERS);
		List<FrameworkMethod> parametersMethods = getParametersMethods();
		createRunnersForParameters(allParameters(parametersMethods), concatNames(parametersMethods));
	}

	private String concatNames(List<FrameworkMethod> parametersMethods) {
		StringBuilder sb = new StringBuilder();
		for (FrameworkMethod method : parametersMethods) {
			Parameterized.Parameters parameters = method.getAnnotation(Parameterized.Parameters.class);
			if (sb.length() != 0) {
				sb.append(", ");
			}
			sb.append(parameters.name());
		}
		return sb.toString();
	}

	@Override
	protected List<Runner> getChildren() {
		return runners;
	}

	private Iterable<Object[]> allParameters(List<FrameworkMethod> parametersMethods) throws Throwable {
		ArrayList<Iterable<Object[]>> returnedParameters = new ArrayList<Iterable<Object[]>>();
		ArrayList<Object[]> allParameters = new ArrayList<Object[]>();
		Object cachedInstance = null;
		for (FrameworkMethod method : parametersMethods) {
			Object parameters;
			if (method.isStatic()) {
				parameters = method.invokeExplosively(null);
			}
			else {
				if (cachedInstance == null) {
					cachedInstance = getTestClass().getOnlyConstructor().newInstance();
				}
				parameters = method.invokeExplosively(cachedInstance);
			}
			if (parameters instanceof Iterable) {
				returnedParameters.add((Iterable<Object[]>) parameters);
			}
			else {
				throw parametersMethodReturnedWrongType(method);
			}
		}
		for (Iterable<Object[]> parameters : returnedParameters) {
			if (allParameters.isEmpty()) {
				for (Object[] array : parameters) {
					allParameters.add(array);
				}
			}
			else {
				ArrayList<Object[]> newAllParameters = new ArrayList<Object[]>();
				for (Object[] prev : allParameters) {
					for (Object[] array : parameters) {
						Object[] next = Arrays.copyOf(prev, prev.length + array.length);
						System.arraycopy(array, 0, next, prev.length, array.length);
						newAllParameters.add(next);
					}
				}
				allParameters = newAllParameters;
			}
		}
		return allParameters;
	}

	private List<FrameworkMethod> getParametersMethods() throws Exception {
		List<FrameworkMethod> methods = getTestClass().getAnnotatedMethods(
				Parameterized.Parameters.class);
		SortedMap<Integer, FrameworkMethod> sortedMethods = new TreeMap<Integer, FrameworkMethod>();
		for (FrameworkMethod each : methods) {
			if (each.isPublic()) {
				if (!each.isStatic()) {
					if (getTestClass().getOnlyConstructor().getParameterCount() != 0) {
						throw new Exception("Method " + each.getMethod() + " is annotated with @Parameters, it is not static and there is no parameter-less constructor!");
					}
				}
				Order order = each.getAnnotation(Order.class);
				int value = order == null ? 0 : order.value();
				FrameworkMethod prev = sortedMethods.put(value, each);
				if (prev != null) {
					throw new Exception(String.format("There are more methods annotated with @Parameters and @Order(value=%d): %s (%s) and %s (%s)",
							value, prev.getMethod(), prev.getAnnotation(Order.class), each.getMethod(), order));
				}
			}
			else {
				throw new Exception("Method " + each.getMethod() + " is annotated with @Parameters but it is not public!");
			}
		}
		if (sortedMethods.isEmpty()) {
			throw new Exception("No public static parameters method on class "
					+ getTestClass().getName());
		}
		return new ArrayList<FrameworkMethod>(sortedMethods.values());
	}

	private void createRunnersForParameters(Iterable<Object[]> allParameters, String namePattern) throws Exception {
		int i = 0;
		for (Object[] parametersOfSingleTest : allParameters) {
			String name = nameFor(namePattern, i, parametersOfSingleTest);
			CustomRunnerForParameters runner = new CustomRunnerForParameters(
					getTestClass().getJavaClass(), parametersOfSingleTest,
					name);
			runners.add(runner);
			++i;
		}
	}

	private String nameFor(String namePattern, int index, Object[] parameters) {
		String finalPattern = namePattern.replaceAll("\\{index\\}",
				Integer.toString(index));
		String name = MessageFormat.format(finalPattern, parameters);
		return "[" + name + "]";
	}

	private Exception parametersMethodReturnedWrongType(FrameworkMethod method) throws Exception {
		String className = getTestClass().getName();
		String methodName = method.getName();
		String message = MessageFormat.format(
				"{0}.{1}() must return an Iterable of arrays.",
				className, methodName);
		return new Exception(message);
	}

	private List<FrameworkField> getAnnotatedFieldsByParameter() {
		return getTestClass().getAnnotatedFields(Parameterized.Parameter.class);
	}

	private boolean fieldsAreAnnotated() {
		return !getAnnotatedFieldsByParameter().isEmpty();
	}

	private class CustomRunnerForParameters extends CustomRunner {
		private final Object[] parameters;
		private final String name;

		CustomRunnerForParameters(Class<?> type, Object[] parameters, String name) throws InitializationError, NoTestsRemainException {
			super(type);
			this.parameters = parameters;
			this.name = name;
		}

		@Override
		protected Object getTestInstance() throws Exception {
			if (testInstance == null) {
				if (fieldsAreAnnotated()) {
					testInstance = createTestUsingFieldInjection();
				}
				else {
					testInstance = createTestUsingConstructorInjection();
				}
			}
			return testInstance;
		}

		private Object createTestUsingConstructorInjection() throws Exception {
			return getTestClass().getOnlyConstructor().newInstance(parameters);
		}

		private Object createTestUsingFieldInjection() throws Exception {
			List<FrameworkField> annotatedFieldsByParameter = getAnnotatedFieldsByParameter();
			if (annotatedFieldsByParameter.size() != parameters.length) {
				throw new Exception("Wrong number of parameters and @Parameter fields." +
						" @Parameter fields counted: " + annotatedFieldsByParameter.size() + ", available parameters: " + parameters.length + ".");
			}
			Object testClassInstance = getTestClass().getJavaClass().newInstance();
			for (FrameworkField each : annotatedFieldsByParameter) {
				Field field = each.getField();
				Parameterized.Parameter annotation = field.getAnnotation(Parameterized.Parameter.class);
				int index = annotation.value();
				try {
					field.set(testClassInstance, parameters[index]);
				}
				catch (IllegalArgumentException iare) {
					throw new Exception(getTestClass().getName() + ": Trying to set " + field.getName() +
							" with the value " + parameters[index] +
							" that is not the right type (" + parameters[index].getClass().getSimpleName() + " instead of " +
							field.getType().getSimpleName() + ").", iare);
				}
			}
			return testClassInstance;
		}

		@Override
		protected String getName() {
			return name;
		}

		@Override
		protected String testName(FrameworkMethod method) {
			return method.getName() + getName();
		}

		@Override
		protected void validateConstructor(List<Throwable> errors) {
			validateOnlyOneConstructor(errors);
			if (fieldsAreAnnotated()) {
				validateZeroArgConstructor(errors);
			}
		}

		@Override
		protected void validateFields(List<Throwable> errors) {
			super.validateFields(errors);
			if (fieldsAreAnnotated()) {
				List<FrameworkField> annotatedFieldsByParameter = getAnnotatedFieldsByParameter();
				int[] usedIndices = new int[annotatedFieldsByParameter.size()];
				for (FrameworkField each : annotatedFieldsByParameter) {
					int index = each.getField().getAnnotation(Parameterized.Parameter.class).value();
					if (index < 0 || index > annotatedFieldsByParameter.size() - 1) {
						errors.add(
								new Exception("Invalid @Parameter value: " + index + ". @Parameter fields counted: " +
										annotatedFieldsByParameter.size() + ". Please use an index between 0 and " +
										(annotatedFieldsByParameter.size() - 1) + ".")
						);
					}
					else {
						usedIndices[index]++;
					}
				}
				for (int index = 0; index < usedIndices.length; index++) {
					int numberOfUse = usedIndices[index];
					if (numberOfUse == 0) {
						errors.add(new Exception("@Parameter(" + index + ") is never used."));
					}
					else if (numberOfUse > 1) {
						errors.add(new Exception("@Parameter(" + index + ") is used more than once (" + numberOfUse + ")."));
					}
				}
			}
		}

		@Override
		protected Statement classBlock(RunNotifier notifier) {
			Statement statement = childrenInvoker(notifier);
			statement = withBeforeClasses(statement);
			statement = withAfterClasses(statement);
			// no class rules executed! These will be executed for the whole suite.
			return statement;
		}

		@Override
		protected Statement withBeforeClasses(Statement statement) {
			if ( isAllTestsIgnored() ) {
				return statement;
			}
			return new BeforeClassCallbackHandler( this, statement );
		}

		@Override
		protected Statement withAfterClasses(Statement statement) {
			if ( isAllTestsIgnored() ) {
				return statement;
			}
			return new AfterClassCallbackHandler( this, statement );
		}

		@Override
		protected Annotation[] getRunnerAnnotations() {
			return new Annotation[0];
		}
	}

	@Retention(value = RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface Order {
		int value();
	}
}
