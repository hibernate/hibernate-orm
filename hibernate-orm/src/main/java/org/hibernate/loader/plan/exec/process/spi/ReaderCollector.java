/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.exec.process.spi;

import java.util.List;

/**
 * Used as a callback mechanism while building the SQL statement to collect the needed ResultSet initializers.
 *
 * @author Steve Ebersole
 * @author Gail Badner
 */
public interface ReaderCollector {
	public ReturnReader getReturnReader();

	public void add(CollectionReferenceInitializer collectionReferenceInitializer);
	public List<CollectionReferenceInitializer> getArrayReferenceInitializers();
	public List<CollectionReferenceInitializer> getNonArrayCollectionReferenceInitializers();

	public void add(EntityReferenceInitializer entityReferenceInitializer);
	public List<EntityReferenceInitializer> getEntityReferenceInitializers();

	public RowReader buildRowReader();
}
