/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.envers.integration.lazy;

import java.util.List;
import javax.persistence.EntityManager;

import org.hibernate.dialect.OracleDialect;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.collection.MultipleCollectionEntity;
import org.hibernate.envers.test.entities.collection.MultipleCollectionRefEntity1;
import org.hibernate.envers.test.entities.collection.MultipleCollectionRefEntity2;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import org.hibernate.Hibernate;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;

/**
 * @author Fabricio Gregorio
 */
@TestForIssue(jiraKey = "HHH-15522")
@SkipForDialect(value = OracleDialect.class, comment = "Oracle does not support identity key generation")
public class IsCollectionInitializedTest extends BaseEnversJPAFunctionalTestCase {

	private Long mce1Id = null;
	private Long mcre1Id = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{
				MultipleCollectionEntity.class, MultipleCollectionRefEntity1.class, MultipleCollectionRefEntity2.class
		};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Revision 1 - addition.
		em.getTransaction().begin();
		MultipleCollectionEntity mce1 = new MultipleCollectionEntity();
		mce1.setText( "MultipleCollectionEntity-1-1" );
		em.persist( mce1 ); // Persisting entity with empty collections.
		em.getTransaction().commit();

		mce1Id = mce1.getId();

		// Revision 2 - update.
		em.getTransaction().begin();
		mce1 = em.find( MultipleCollectionEntity.class, mce1.getId() );
		MultipleCollectionRefEntity1 mcre1 = new MultipleCollectionRefEntity1();
		mcre1.setText( "MultipleCollectionRefEntity1-1-1" );
		mcre1.setMultipleCollectionEntity( mce1 );
		mce1.addRefEntity1( mcre1 );
		em.persist( mcre1 );
		mce1 = em.merge( mce1 );
		em.getTransaction().commit();

		mcre1Id = mcre1.getId();

		em.close();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testIsInitialized() {
		EntityManager em = getEntityManager();

		AuditReader reader = AuditReaderFactory.get( em );
		List<MultipleCollectionEntity> res = reader.createQuery().forEntitiesAtRevision( MultipleCollectionEntity.class, 1 )
				.add( AuditEntity.id().eq( mce1Id ) )
				.getResultList();

		MultipleCollectionEntity ret = res.get( 0 );
		
		assertEquals( Hibernate.isInitialized( ret.getRefEntities1() ), false );
		
		Hibernate.initialize(ret.getRefEntities1());
		
		assertEquals( Hibernate.isInitialized( ret.getRefEntities1() ), true );

	}
}
