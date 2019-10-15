/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.junit4.runtimecheck;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import org.hibernate.testing.junit4.CustomRunner;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runners.model.InitializationError;

import org.jboss.byteman.contrib.bmunit.BMScript;
import org.jboss.byteman.contrib.bmunit.BMUnitConfig;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;

/**
 * Executes by default the Byteman rules to check whether some forbidden APIs are invoked at runtime.
 * Requires that the Byteman rules file {@link #RULE_FILE_NAME} will be present in the {@link #RULE_FILE_PATH}.
 * <p>
 * To skip the rules execution it's enough to annotate the test class with {@link SkipBMRuntimeCheck}.
 *
 * @author Fabio Massimo Ercoli
 */
public class BMRuntimeCheckCustomRunner extends CustomRunner {

	private static final String RULE_FILE_NAME = "runtime-check.btm";
	private static final String RULE_FILE_PATH = "target/resources/test";

	private static final RuntimeCheckBMUnitConfig RUNTIME_CHECK_BM_UNIT_CONFIG = new RuntimeCheckBMUnitConfig();
	private static final RuntimeCheckBMScript RUNTIME_CHECK_BM_SCRIPT = new RuntimeCheckBMScript();

	public BMRuntimeCheckCustomRunner(Class<?> clazz) throws InitializationError, NoTestsRemainException {
		super( clazz );
		if ( isRulesExecutionSkipped() ) {
			return;
		}

		// TODO HHH-13604 Temporary loading the rules only for tests from this package:
		if ( clazz.getPackage() == null || !"org.hibernate.session.runtime.check".equals( clazz.getPackage().getName() ) ) {
			return;
		}

		// TODO HHH-13604 Even the smoke test does not work with the annotation read rule
		if ( "CheckForbiddenAPIAtRuntimeTest".equals( clazz.getSimpleName() ) ) {
			return;
		}

		try {
			setAnnotations();
		}
		catch (NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException( "Error applying BM annotations at runtime to check runtime rules", e );
		}
	}

	private void setAnnotations() throws NoSuchFieldException, IllegalAccessException {
		Field classConfigAnnotationField = BMUnitRunner.class.getDeclaredField( "classConfigAnnotation" );
		classConfigAnnotationField.setAccessible( true );
		classConfigAnnotationField.set( this, RUNTIME_CHECK_BM_UNIT_CONFIG );

		Field classSingleScriptAnnotationField = BMUnitRunner.class.getDeclaredField( "classSingleScriptAnnotation" );
		classSingleScriptAnnotationField.setAccessible( true );
		classSingleScriptAnnotationField.set( this, RUNTIME_CHECK_BM_SCRIPT );
	}

	/**
	 * Whether the rules execution is skipped for the class by {@link SkipBMRuntimeCheck}.
	 */
	private boolean isRulesExecutionSkipped() {
		SkipBMRuntimeCheck skipBMRuntimeCheck = getTestClass().getJavaClass().getAnnotation( SkipBMRuntimeCheck.class );
		return skipBMRuntimeCheck != null;
	}

	private static class RuntimeCheckBMUnitConfig implements BMUnitConfig {

		@Override
		public boolean enforce() {
			return false;
		}

		@Override
		public String agentHost() {
			return "";
		}

		@Override
		public String agentPort() {
			return "";
		}

		@Override
		public boolean inhibitAgentLoad() {
			return false;
		}

		@Override
		public String loadDirectory() {
			return RULE_FILE_PATH;
		}

		@Override
		public String resourceLoadDirectory() {
			return "";
		}

		@Override
		public boolean allowAgentConfigUpdate() {
			return true;
		}

		@Override
		public boolean verbose() {
			return false;
		}

		@Override
		public boolean debug() {
			return true;
		}

		@Override
		public boolean bmunitVerbose() {
			return false;
		}

		@Override
		public boolean policy() {
			return false;
		}

		@Override
		public boolean dumpGeneratedClasses() {
			return false;
		}

		@Override
		public String dumpGeneratedClassesDirectory() {
			return "";
		}

		@Override
		public boolean dumpGeneratedClassesIntermediate() {
			return false;
		}

		@Override
		public Class<? extends Annotation> annotationType() {
			return BMUnitConfig.class;
		}
	}

	private static class RuntimeCheckBMScript implements BMScript {

		@Override
		public String value() {
			return RULE_FILE_NAME;
		}

		@Override
		public String dir() {
			return "";
		}

		@Override
		public Class<? extends Annotation> annotationType() {
			return BMScript.class;
		}
	}

}
