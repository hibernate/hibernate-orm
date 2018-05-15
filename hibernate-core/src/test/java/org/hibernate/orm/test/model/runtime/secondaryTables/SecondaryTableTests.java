/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.model.runtime.secondaryTables;

import org.hibernate.boot.MetadataSources;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.relational.spi.ForeignKey;
import org.hibernate.metamodel.model.relational.spi.JoinedTableBinding;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.orm.test.SessionFactoryBasedFunctionalTest;
import org.hibernate.orm.test.support.domains.contacts.Contact;
import org.hibernate.orm.test.support.domains.contacts.ModelClasses;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.hamcrest.CollectionMatchers.hasSize;

/**
 * @author Steve Ebersole
 */
public class SecondaryTableTests extends SessionFactoryBasedFunctionalTest {
	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		ModelClasses.applyContactsModel( metadataSources );
	}

	@Test
	public void basicSecondaryTableTest() {
		final EntityTypeDescriptor<? extends Contact> contactDescriptor = sessionFactory().getMetamodel()
				.findEntityDescriptor( Contact.class );
		assertThat( contactDescriptor.getPrimaryTable(), notNullValue() );
		assertThat( contactDescriptor.getSecondaryTableBindings(), hasSize( 1 ) );

		final JoinedTableBinding secondaryTableJoin = contactDescriptor.getSecondaryTableBindings().get( 0 );

		// for secondary tables, the secondary table is actually the referring table
		assertThat( secondaryTableJoin.getTargetTable(), is( contactDescriptor.getPrimaryTable() ) );
		final Table secondaryTable = secondaryTableJoin.getReferringTable();
		assertThat( secondaryTable.getForeignKeys(), hasSize( 1 ) );
		final ForeignKey fk = secondaryTable.getForeignKeys().iterator().next();
		assertThat( fk.getReferringTable(), is( secondaryTable) );
		assertThat( fk.getTargetTable(), is( contactDescriptor.getPrimaryTable() ) );
		assertThat( fk.getColumnMappings().getColumnMappings().size(), is( contactDescriptor.getPrimaryTable().getPrimaryKey().getColumns().size() ) );
		assertThat( fk.getColumnMappings().getColumnMappings().size(), is( secondaryTable.getPrimaryKey().getColumns().size() ) );

	}
}
