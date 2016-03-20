package org.hibernate.jpa.test.graphs.named.subgraph;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

import javax.persistence.AttributeNode;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import java.util.List;

public class SubgraphOrmNamedEntityGraphTest extends BaseEntityManagerFunctionalTestCase {
//	@Override
//	protected Class<?>[] getAnnotatedClasses() {
//		return new Class[] { Author.class, Book.class };
//	}
	
	@Override
	public String[] getEjb3DD() {
		return new String[]{"org/hibernate/jpa/test/graphs/named/subgraph/orm.xml"};
	}
	@Test
	public void hhh10633Test() throws Exception {
		EntityManager entityManager = getOrCreateEntityManager();
		List<EntityGraph<? super Book>> lneg = entityManager.getEntityGraphs( Book.class );
		
		Assert.assertEquals(2, lneg.size());
		for (EntityGraph<? super Book> neg : lneg){
			if (neg.getName().equalsIgnoreCase( "full" )){
				Assert.assertNotNull( neg.getAttributeNodes() );
				for (AttributeNode<?> n : neg.getAttributeNodes()){
					if (n.getAttributeName().equalsIgnoreCase( "authors" )) {
						Assert.assertEquals(1, n.getSubgraphs().size());
						java.util.List<javax.persistence.AttributeNode<?>> attributeNodes = n.getSubgraphs().get(Author.class).getAttributeNodes();
						Assert.assertNotNull("Subgraph attributes missing", attributeNodes);
						Assert.assertEquals("Subgraph wrong number of attributes ", 3, attributeNodes.size());
					}
				}
			}
		}
		entityManager.close();
	}
}
