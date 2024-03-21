/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.bind.spi;

import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.models.bind.internal.IdentifiableTypeBinding;
import org.hibernate.boot.models.bind.internal.ManagedTypeBinding;
import org.hibernate.boot.models.bind.internal.SecondaryTable;
import org.hibernate.boot.models.categorize.spi.FilterDefRegistration;
import org.hibernate.boot.models.categorize.spi.ManagedTypeMetadata;
import org.hibernate.boot.models.categorize.spi.TableOwner;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.internal.util.KeyedConsumer;
import org.hibernate.models.spi.ClassDetails;

/**
 * The idea here is mostly the role filled by InFlightMetadataCollector upstream.
 * We should incorporate any improvements here into there when we move this upstream.
 *
 * @author Steve Ebersole
 */
public interface BindingState {
	MetadataBuildingContext getMetadataBuildingContext();

	default Database getDatabase() {
		return getMetadataBuildingContext().getMetadataCollector().getDatabase();
	}

	JdbcServices getJdbcServices();

	void apply(FilterDefRegistration registration);

	int getTableCount();
	void forEachTable(KeyedConsumer<String,TableReference> consumer);
	<T extends TableReference> T getTableByName(String name);
	<T extends TableReference> T getTableByOwner(TableOwner owner);
	void addTable(TableOwner owner, TableReference table);
	void addSecondaryTable(SecondaryTable table);


	void registerTypeBinding(ManagedTypeMetadata type, ManagedTypeBinding binding);
	ManagedTypeBinding getTypeBinding(ClassDetails type);
	default ManagedTypeBinding getTypeBinding(ManagedTypeMetadata type) {
		return getTypeBinding( type.getClassDetails() );
	}

	IdentifiableTypeBinding getSuperTypeBinding(ClassDetails type);
}
