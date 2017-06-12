/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.relational.spi;

import java.util.Collection;

import org.hibernate.boot.model.relational.InitCommand;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

/**
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
public interface DatabaseModel {
	Collection<Namespace> getNamespaces();

	Namespace getDefaultNamespace();

	JdbcEnvironment getJdbcEnvironment();

	Collection<AuxiliaryDatabaseObject> getAuxiliaryDatabaseObjects();

	Collection<InitCommand> getInitCommands();

	void addInitCommand(InitCommand initCommand);
}
