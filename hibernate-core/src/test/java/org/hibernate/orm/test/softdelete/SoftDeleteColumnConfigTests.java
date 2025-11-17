/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.softdelete;

import org.hibernate.annotations.SoftDelete;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.RootClass;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.schema.SchemaCreateHelper;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test column configuration though {@linkplain SoftDelete @SoftDelete}
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class SoftDeleteColumnConfigTests {
	@Test
	@DomainModel(annotatedClasses = Thing.class)
	@RequiresDialect( value = H2Dialect.class, comment = "Not all dialects export column comments, and we only really need to check using one that does" )
	void verifyModel(DomainModelScope modelScope) {
		final RootClass entityBinding = (RootClass) modelScope.getEntityBinding( Thing.class );
		final Column softDeleteColumn = entityBinding.getSoftDeleteColumn();
		assertThat( softDeleteColumn.getOptions() ).isEqualTo( "do_it=true" );
		assertThat( softDeleteColumn.getComment() ).isEqualTo( "Explicit soft-delete comment" );

		final String ddl = SchemaCreateHelper.toCreateDdl( modelScope.getDomainModel() );
		assertThat( ddl ).contains( "do_it=true" );
		assertThat( ddl ).contains( "Explicit soft-delete comment" );
	}

	@Entity(name="Thing")
	@Table(name="Thing")
	@SoftDelete( comment = "Explicit soft-delete comment", options = "do_it=true" )
	public static class Thing {
		@Id
		private Integer id;
		private String name;
	}
}
