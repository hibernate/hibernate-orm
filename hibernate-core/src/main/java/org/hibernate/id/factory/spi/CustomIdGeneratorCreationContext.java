/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.id.factory.spi;

import org.hibernate.Incubating;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.mapping.RootClass;

@Incubating
public interface CustomIdGeneratorCreationContext extends GeneratorCreationContext {
	RootClass getRootClass();

	// we could add these if it helps integrate old infrastructure
//	Properties getParameters();
//	SqlStringGenerationContext getSqlStringGenerationContext();
}
