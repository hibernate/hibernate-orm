/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.cfg;

import java.util.Properties;

import org.hibernate.bytecode.internal.none.BytecodeProviderImpl;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.internal.CoreMessageLogger;

import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.junit.Assert.assertTrue;

/**
 * @author Réda Housni Alaoui
 */
public class EnvironmentTest {

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule(
			Logger.getMessageLogger( CoreMessageLogger.class, Environment.class.getName() )
	);

	@Test
	public void testCustomBytecodeProviderBuild() {
		Properties properties = new Properties();
		properties.put( AvailableSettings.BYTECODE_PROVIDER, BytecodeProviderImpl.class.getName() );

		BytecodeProvider bytecodeProvider = Environment.buildBytecodeProvider( properties );

		assertTrue( bytecodeProvider instanceof BytecodeProviderImpl );
	}

	@Test
	public void testCustomBytecodeProviderClassNotFound() {
		Properties properties = new Properties();
		properties.put( AvailableSettings.BYTECODE_PROVIDER, "N/A" );

		Triggerable triggerable = logInspection.watchForLogMessages( "HHH000500" );

		BytecodeProvider bytecodeProvider = Environment.buildBytecodeProvider( properties );
		assertTrue( bytecodeProvider instanceof org.hibernate.bytecode.internal.bytebuddy.BytecodeProviderImpl );
		assertTrue( triggerable.wasTriggered() );
	}

	@Test
	public void testCustomBytecodeProviderClassCastFailure() {
		Properties properties = new Properties();
		properties.put( AvailableSettings.BYTECODE_PROVIDER, this.getClass().getName() );

		Triggerable triggerable = logInspection.watchForLogMessages( "HHH000501" );

		BytecodeProvider bytecodeProvider = Environment.buildBytecodeProvider( properties );
		assertTrue( bytecodeProvider instanceof org.hibernate.bytecode.internal.bytebuddy.BytecodeProviderImpl );
		assertTrue( triggerable.wasTriggered() );
	}
}