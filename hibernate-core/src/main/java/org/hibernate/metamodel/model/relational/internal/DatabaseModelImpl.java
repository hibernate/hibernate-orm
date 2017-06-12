/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.metamodel.model.relational.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hibernate.boot.model.relational.InitCommand;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.metamodel.model.relational.spi.AuxiliaryDatabaseObject;
import org.hibernate.metamodel.model.relational.spi.DatabaseModel;
import org.hibernate.metamodel.model.relational.spi.Namespace;

/**
 * @author Steve Ebersole
 */
public class DatabaseModelImpl implements DatabaseModel {
	private final JdbcEnvironment getJdbcEnvironment;

	private Namespace defautlNamespace;
	private List<AuxiliaryDatabaseObject> auxiliaryDatabaseObjects;
	private final List<Namespace> namespaces = new ArrayList<>();
	private List<InitCommand> initCommands = new ArrayList<>();

	public DatabaseModelImpl(JdbcEnvironment getJdbcEnvironment) {
		this.getJdbcEnvironment = getJdbcEnvironment;
	}

	@Override
	public Collection<Namespace> getNamespaces() {
		return namespaces;
	}

	@Override
	public Namespace getDefaultNamespace() {
		return defautlNamespace;
	}

	@Override
	public JdbcEnvironment getJdbcEnvironment() {
		return getJdbcEnvironment;
	}

	@Override
	public Collection<AuxiliaryDatabaseObject> getAuxiliaryDatabaseObjects() {
		if ( auxiliaryDatabaseObjects == null ) {
			return Collections.emptyList();
		}
		return auxiliaryDatabaseObjects;
	}

	@Override
	public Collection<InitCommand> getInitCommands() {
		return initCommands;
	}

	@Override
	public void addInitCommand(InitCommand initCommand) {
		initCommands.add( initCommand );
	}

	public void addNamespace(Namespace namespace) {
		namespaces.add( namespace );
	}

	public void setDefaultNamespace(Namespace namespace){
		defautlNamespace = namespace;
	}

	public void addAuxiliaryDatabaseObject(AuxiliaryDatabaseObject auxiliaryDatabaseObject) {
		if ( auxiliaryDatabaseObjects == null ) {
			auxiliaryDatabaseObjects = new ArrayList<>();
		}
		this.auxiliaryDatabaseObjects.add( auxiliaryDatabaseObject );
	}
}
