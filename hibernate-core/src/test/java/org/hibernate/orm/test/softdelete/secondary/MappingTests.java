/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.softdelete.secondary;

import java.util.Set;

import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.orm.test.softdelete.MappingVerifier;
import org.hibernate.orm.test.util.SchemaUtil;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = { JoinedRoot.class, JoinedSub.class })
@SessionFactory()
public class MappingTests {
	@Test
	void verifyMapping(SessionFactoryScope scope, DomainModelScope dmScope) {
		final MappingMetamodelImplementor metamodel = scope.getSessionFactory().getMappingMetamodel();

		MappingVerifier.verifyMapping(
				metamodel.getEntityDescriptor( JoinedRoot.class ).getSoftDeleteMapping(),
				"removed",
				"joined_root",
				'Y'
		);

		MappingVerifier.verifyMapping(
				metamodel.getEntityDescriptor( JoinedSub.class ).getSoftDeleteMapping(),
				"removed",
				"joined_root",
				'Y'
		);

		final Set<String> rootTableColumns = SchemaUtil.getColumnNames( "joined_root", dmScope.getDomainModel() );
		assertThat( rootTableColumns ).contains( "removed" );

		final Set<String> subTableColumns = SchemaUtil.getColumnNames( "joined_sub", dmScope.getDomainModel() );
		assertThat( subTableColumns ).doesNotContain( "removed" );
	}
}
