//$Id$
package org.hibernate.test.annotations.manytoone;

import org.hibernate.test.annotations.TestCase;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 * @author Emmanuel Bernard
 */
public class ManyToOneJoinTest extends TestCase {
	public void testManyToOneJoinTable() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		ForestType forest = new ForestType();
		forest.setName( "Original forest" );
		s.persist( forest );
		TreeType tree = new TreeType();
		tree.setForestType( forest );
		tree.setAlternativeForestType( forest );
		tree.setName( "just a tree");
		s.persist( tree );
		s.flush();
		s.clear();
		tree = (TreeType) s.get(TreeType.class, tree.getId() );
		assertNotNull( tree.getForestType() );
		assertNotNull( tree.getAlternativeForestType() );
		s.clear();
		forest = (ForestType) s.get( ForestType.class, forest.getId() );
		assertEquals( 1, forest.getTrees().size() );
		assertEquals( tree.getId(), forest.getTrees().iterator().next().getId() );
		tx.rollback();
		s.close();
	}

	public void testOneToOneJoinTable() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		ForestType forest = new ForestType();
		forest.setName( "Original forest" );
		s.persist( forest );
		BiggestForest forestRepr = new BiggestForest();
		forestRepr.setType( forest );
		forest.setBiggestRepresentative( forestRepr );
		s.persist( forestRepr );
		s.flush();
		s.clear();
		forest = (ForestType) s.get( ForestType.class, forest.getId() );
		assertNotNull( forest.getBiggestRepresentative() );
		assertEquals( forest, forest.getBiggestRepresentative().getType() );
		tx.rollback();
		s.close();
	}

	protected Class[] getMappings() {
		return new Class[] {
				BiggestForest.class,
				ForestType.class,
				TreeType.class
		};
	}
}
