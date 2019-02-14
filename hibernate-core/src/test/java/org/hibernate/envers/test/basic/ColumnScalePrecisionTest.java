/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.basic;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.envers.test.EnversSessionFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.ScalePrecisionEntity;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeBasic;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.NonIdPersistentAttribute;
import org.hibernate.metamodel.model.relational.spi.Column;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-7003")
public class ColumnScalePrecisionTest extends EnversSessionFactoryBasedFunctionalTest {
	private static final String ENTITY_NAME = ScalePrecisionEntity.class.getName();
	private static final String AUDIT_ENTITY_NAME = ENTITY_NAME + "_AUD";

	private Long id = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { ScalePrecisionEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		this.id = inTransaction(
				session -> {
					ScalePrecisionEntity entity = new ScalePrecisionEntity( 13.0 );
					session.save( entity );
					return entity.getId();
				}
		);
	}

	@DynamicTest
	public void testColumnScalePrecision() {
		Column auditedColumn = getScalePrecisionColumnFromEntity( AUDIT_ENTITY_NAME );
		assertThat( auditedColumn, notNullValue() );

		Column column = getScalePrecisionColumnFromEntity( ENTITY_NAME );
		assertThat( column, notNullValue() );

		assertThat( auditedColumn.getSize().getPrecision(), equalTo( column.getSize().getPrecision() ) );
		assertThat( auditedColumn.getSize().getScale(), equalTo( column.getSize().getScale() ) );
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( ScalePrecisionEntity.class, id ), contains( 1 ) );
	}

	@DynamicTest
	public void testHistoryOfScalePrecisionEntity() {
		ScalePrecisionEntity ver1 = new ScalePrecisionEntity( 13.0, id );
		assertThat( getAuditReader().find( ScalePrecisionEntity.class, id, 1 ), equalTo( ver1 ) );
	}

	private Column getScalePrecisionColumnFromEntity(String entityName) {
		// Lookup the descriptor by name.
		final EntityTypeDescriptor<?> descriptor = sessionFactoryScope()
				.getSessionFactory()
				.getMetamodel()
				.entity( entityName );

		// Iterate columns and collect those aptly named
		List<Column> columns = new ArrayList<>();
		for ( NonIdPersistentAttribute attribute : descriptor.getPersistentAttributes() ) {
			if ( attribute.getNavigableName().equals( "wholeNumber" ) ) {
				assertThat( attribute, instanceOf( SingularPersistentAttributeBasic.class ) );
				return ( (SingularPersistentAttributeBasic) attribute ).getBoundColumn();
			}
		}

		return null;
	}
}
