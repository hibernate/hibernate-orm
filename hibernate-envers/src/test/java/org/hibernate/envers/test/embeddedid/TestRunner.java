package org.hibernate.envers.test.embeddedid;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.envers.test.BaseEnversFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.testing.TestForIssue;
import org.junit.Before;
import org.junit.Test;

@TestForIssue(jiraKey = "HHH-13361")
public class TestRunner extends BaseEnversFunctionalTestCase {

	protected SessionFactory sessionFactory;

	@Override
	protected String[] getMappings() {
		return new String[]{
				"mappings/embeddedComponents/embedded.hbm.xml"
		};
	}

	@Before
	public void setup() {
		final StandardServiceRegistry registry = new StandardServiceRegistryBuilder().configure() // configures
				// hibernate.cfg.xml
				.build();
		try {
			sessionFactory = new MetadataSources( registry ).buildMetadata().buildSessionFactory();
		}
		catch (Exception ex) {
			ex.printStackTrace();

			StandardServiceRegistryBuilder.destroy( registry );
		}
	}

	@Test
	@Priority(1)
	public void testInsertRelationCode() {

		Session session = sessionFactory.openSession();
		session.beginTransaction();
		OwnerOfRelationCode instance = new OwnerOfRelationCode();
		CompositeEntity compositeEntity = new CompositeEntity();
		compositeEntity.setFirstCode( "firstCode" );
		compositeEntity.setSecondCode( "secondCode" );
		session.save( compositeEntity );

		instance.setCompositeEntity( compositeEntity );
		instance.setSecondIdentifier( "secondIdentifier" );

		session.save( instance );

		session.getTransaction().commit();
		session.close();
		sessionFactory.close();
	}

	@Test
	@Priority(2)
	public void testUpdateRelationCode() {
		Session session = sessionFactory.openSession();
		session.beginTransaction();

		OwnerOfRelationCodeId idClass = new OwnerOfRelationCodeId();
		CompositeEntityId compositeEntityId = new CompositeEntityId();
		compositeEntityId.setFirstCode( "firstCode" );
		compositeEntityId.setSecondCode( "secondCode" );
		idClass.setCompositeEntity( compositeEntityId );

		OwnerOfRelationCode ownerOfRelationCode = session.get( OwnerOfRelationCode.class, idClass );
		ownerOfRelationCode.setDescription( "first description" );

		session.close();
		sessionFactory.close();

	}

}
