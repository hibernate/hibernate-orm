/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.instrument.javassist;

import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.tool.enhance.EnhancementTask;

/**
 * This is the legacy Ant-task Hibernate provided historically to
 * perform its old-school bytecode instrumentation.  That has been replaced wholesale
 * with a new approach to bytecode manipulation offering 3 build-time variations for Ant,
 * Maven and Gradle.
 *
 * @author Muga Nishizawa
 * @author Steve Ebersole
 *
 * @deprecated This is the legacy Ant-task Hibernate provided historically to
 * perform its old-school bytecode instrumentation.  That has been replaced wholesale
 * with a new approach to bytecode manipulation offering 3 build-time variations for Ant,
 * Maven and Gradle.
 *
 * @see EnhancementTask
 */
@Deprecated
@SuppressWarnings("unused")
public class InstrumentTask extends EnhancementTask {
	public InstrumentTask() {
		DeprecationLogger.DEPRECATION_LOGGER.logDeprecatedInstrumentTask( InstrumentTask.class, EnhancementTask.class );
	}
}
