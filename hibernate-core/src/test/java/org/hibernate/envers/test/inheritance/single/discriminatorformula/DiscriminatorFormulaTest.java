/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.inheritance.single.discriminatorformula;

import java.util.List;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.inheritance.single.discriminatorformula.ChildEntity;
import org.hibernate.envers.test.support.domains.inheritance.single.discriminatorformula.ClassTypeEntity;
import org.hibernate.envers.test.support.domains.inheritance.single.discriminatorformula.ParentEntity;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.DerivedColumn;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Disabled("NYI - Inheritance support")
public class DiscriminatorFormulaTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Long childTypeId;
	private Long parentTypeId;
	private Long childId;
	private Long parentId;

	private ChildEntity childVer1 = null;
	private ChildEntity childVer2 = null;
	private ParentEntity parentVer1 = null;
	private ParentEntity parentVer2 = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { ClassTypeEntity.class, ParentEntity.class, ChildEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Child entity type
				entityManager -> {
					ClassTypeEntity childType = new ClassTypeEntity();
					childType.setType( ClassTypeEntity.CHILD_TYPE );
					entityManager.persist( childType );
					childTypeId = childType.getId();
				},

				// Parent entity type
				entityManager -> {
					ClassTypeEntity parentType = new ClassTypeEntity();
					parentType.setType( ClassTypeEntity.PARENT_TYPE );
					entityManager.persist( parentType );
					parentTypeId = parentType.getId();
				},

				// Child Rev 1
				entityManager -> {
					ChildEntity child = new ChildEntity( childTypeId, "Child data", "Child specific data" );
					entityManager.persist( child );
					childId = child.getId();
				},

				// Parent Rev 2
				entityManager -> {
					ParentEntity parent = new ParentEntity( parentTypeId, "Parent data" );
					entityManager.persist( parent );
					parentId = parent.getId();
				},

				// Child Rev 3
				entityManager -> {
					ChildEntity child = entityManager.find( ChildEntity.class, childId );
					child.setData( "Child data modified" );
				},

				// Parent Rev 4
				entityManager -> {
					ParentEntity parent = entityManager.find( ParentEntity.class, parentId );
					parent.setData( "Parent data modified" );
				}
		);

		childVer1 = new ChildEntity( childId, childTypeId, "Child data", "Child specific data" );
		childVer2 = new ChildEntity( childId, childTypeId, "Child data modified", "Child specific data" );
		parentVer1 = new ParentEntity( parentId, parentTypeId, "Parent data" );
		parentVer2 = new ParentEntity( parentId, parentTypeId, "Parent data modified" );
	}

	@DynamicTest
	public void testDiscriminatorFormulaInAuditTable() {
		EntityTypeDescriptor<?> parentAudit = getAuditEntityDescriptor( ParentEntity.class );

		final Column discriminatorColumn = parentAudit.getHierarchy().getDiscriminatorDescriptor().getBoundColumn();
		assertThat( discriminatorColumn, instanceOf( DerivedColumn.class ) );
		assertThat( discriminatorColumn.getExpression(), equalTo( ParentEntity.DISCRIMINATOR_QUERY ) );
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( ChildEntity.class, childId ), contains( 1, 3 ) );
		assertThat( getAuditReader().getRevisions( ParentEntity.class, parentId ), contains( 2, 4 ) );
	}

	@DynamicTest
	public void testHistoryOfParent() {
		assertThat( getAuditReader().find( ParentEntity.class, parentId, 2 ), equalTo( parentVer1 ) );
		assertThat( getAuditReader().find( ParentEntity.class, parentId, 4 ), equalTo( parentVer2 ) );
	}

	@DynamicTest
	public void testHistoryOfChild() {
		assertThat( getAuditReader().find( ChildEntity.class, childId, 1 ), equalTo( childVer1 ) );
		assertThat( getAuditReader().find( ChildEntity.class, childId, 3 ), equalTo( childVer2 ) );
	}

	@DynamicTest
	@SuppressWarnings("unchecked")
	public void testPolymorphicQuery() {
		assertThat(
				getAuditReader()
						.createQuery()
						.forEntitiesAtRevision( ChildEntity.class, 1 )
						.getSingleResult(),
				equalTo( childVer1 )
		);

		assertThat(
				getAuditReader()
						.createQuery()
						.forEntitiesAtRevision( ParentEntity.class, 1 )
						.getSingleResult(),
				equalTo( childVer1 )
		);

		assertThat(
				(List<ChildEntity>) getAuditReader()
						.createQuery()
						.forRevisionsOfEntity( ChildEntity.class, true, false )
						.getResultList(),
				contains( childVer1, childVer2 )
		);

		assertThat(
				(List<ParentEntity>) getAuditReader()
						.createQuery()
						.forRevisionsOfEntity( ParentEntity.class, true, false )
						.getResultList(),
				contains( childVer1, parentVer1, childVer2, parentVer2 )
		);
	}
}
