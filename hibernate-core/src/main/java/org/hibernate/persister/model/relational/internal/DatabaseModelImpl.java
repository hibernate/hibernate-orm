/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.persister.model.relational.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.persister.model.relational.spi.DatabaseModel;
import org.hibernate.persister.model.relational.spi.Namespace;

/**
 * @author Steve Ebersole
 */
public class DatabaseModelImpl implements DatabaseModel {
	private final List<Namespace> namespaces = new ArrayList<>();

	@Override
	public Collection<Namespace> getNameSpaces() {
		return namespaces;
	}

	public void addNamespace(Namespace namespace) {
		namespaces.add( namespace );
	}
}
