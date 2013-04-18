/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.loader.plan.spi;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.LockMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.spi.ResultSetProcessingContext;

/**
 * @author Steve Ebersole
 */
public class CollectionReturn extends AbstractCollectionReference implements Return, CopyableReturn {
	private final String ownerEntityName;
	private final String ownerProperty;

	public CollectionReturn(
			SessionFactoryImplementor sessionFactory,
			LockMode lockMode,
			String ownerEntityName,
			String ownerProperty) {
		super(
				sessionFactory,
				lockMode,
				sessionFactory.getCollectionPersister( ownerEntityName + '.' + ownerProperty ),
				new PropertyPath() // its a root
		);
		this.ownerEntityName = ownerEntityName;
		this.ownerProperty = ownerProperty;
	}

	public CollectionReturn(CollectionReturn original, CopyContext copyContext) {
		super( original, copyContext );
		this.ownerEntityName = original.ownerEntityName;
		this.ownerProperty = original.ownerProperty;
	}

	/**
	 * Returns the class owning the collection.
	 *
	 * @return The class owning the collection.
	 */
	public String getOwnerEntityName() {
		return ownerEntityName;
	}

	/**
	 * Returns the name of the property representing the collection from the {@link #getOwnerEntityName}.
	 *
	 * @return The name of the property representing the collection on the owner class.
	 */
	public String getOwnerProperty() {
		return ownerProperty;
	}

	@Override
	public void hydrate(ResultSet resultSet, ResultSetProcessingContext context) throws SQLException {
		// todo : anything to do here?
	}

	@Override
	public void resolve(ResultSet resultSet, ResultSetProcessingContext context) throws SQLException {
		// todo : anything to do here?
	}

	@Override
	public Object read(ResultSet resultSet, ResultSetProcessingContext context) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public String toString() {
		return "CollectionReturn(" + getCollectionPersister().getRole() + ")";
	}

	@Override
	public CollectionReturn makeCopy(CopyContext copyContext) {
		return new CollectionReturn( this, copyContext );
	}
}
