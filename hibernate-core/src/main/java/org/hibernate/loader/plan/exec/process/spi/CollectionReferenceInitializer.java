/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.loader.plan.exec.process.spi;

import java.sql.ResultSet;

import org.hibernate.loader.plan.exec.process.internal.ResultSetProcessingContextImpl;
import org.hibernate.loader.plan.spi.CollectionReference;

/**
 * @author Steve Ebersole
 */
public interface CollectionReferenceInitializer {
	// again, not sure.  ResultSetProcessingContextImpl.initializeEntitiesAndCollections() stuff?
	void finishUpRow(ResultSet resultSet, ResultSetProcessingContextImpl context);

	CollectionReference getCollectionReference();

	void endLoading(ResultSetProcessingContextImpl context);
}
