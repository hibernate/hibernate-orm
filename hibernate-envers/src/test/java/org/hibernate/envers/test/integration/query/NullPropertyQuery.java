package org.hibernate.envers.test.integration.query;

import javax.persistence.EntityManager;

import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrIntTestEntity;
import org.hibernate.envers.test.entities.ids.EmbId;
import org.hibernate.envers.test.entities.onetomany.CollectionRefEdEntity;
import org.hibernate.envers.test.entities.onetomany.CollectionRefIngEntity;
import org.hibernate.envers.test.entities.onetomany.ids.SetRefEdEmbIdEntity;
import org.hibernate.envers.test.entities.onetomany.ids.SetRefIngEmbIdEntity;

import org.junit.Test;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class NullPropertyQuery extends BaseEnversJPAFunctionalTestCase {
	private Integer idSimplePropertyNull = null;
	private Integer idSimplePropertyNotNull = null;
	private EmbId idMulticolumnReferenceToParentNull = new EmbId( 0, 1 );
	private Integer idReferenceToParentNotNull = 1;
	private Integer idParent = 1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				StrIntTestEntity.class,
				SetRefEdEmbIdEntity.class,
				SetRefIngEmbIdEntity.class,
				CollectionRefEdEntity.class,
				CollectionRefIngEntity.class,
				EmbId.class
		};
	}

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		StrIntTestEntity nullSite = new StrIntTestEntity( null, 1 );
		StrIntTestEntity notNullSite = new StrIntTestEntity( "data", 2 );
		em.persist( nullSite );
		em.persist( notNullSite );
		idSimplePropertyNull = nullSite.getId();
		idSimplePropertyNotNull = notNullSite.getId();
		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();
		SetRefIngEmbIdEntity nullParentSrieie = new SetRefIngEmbIdEntity(
				idMulticolumnReferenceToParentNull,
				"data",
				null
		);
		em.persist( nullParentSrieie );
		em.getTransaction().commit();

		// Revision 3
		em.getTransaction().begin();
		CollectionRefEdEntity parent = new CollectionRefEdEntity( idParent, "data" );
		CollectionRefIngEntity notNullParentCrie = new CollectionRefIngEntity(
				idReferenceToParentNotNull,
				"data",
				parent
		);
		em.persist( parent );
		em.persist( notNullParentCrie );
		em.getTransaction().commit();
	}

	@Test
	public void testSimplePropertyIsNullQuery() {
		StrIntTestEntity ver = (StrIntTestEntity) getAuditReader().createQuery()
				.forEntitiesAtRevision( StrIntTestEntity.class, 1 )
				.add( AuditEntity.property( "str1" ).isNull() )
				.getSingleResult();

		assert ver.equals( new StrIntTestEntity( null, 1, idSimplePropertyNull ) );
	}

	@Test
	public void testSimplePropertyIsNotNullQuery() {
		StrIntTestEntity ver = (StrIntTestEntity) getAuditReader().createQuery()
				.forEntitiesAtRevision( StrIntTestEntity.class, 1 )
				.add( AuditEntity.property( "str1" ).isNotNull() )
				.getSingleResult();

		assert ver.equals( new StrIntTestEntity( "data", 2, idSimplePropertyNotNull ) );
	}

	@Test
	public void testReferenceMulticolumnPropertyIsNullQuery() {
		SetRefIngEmbIdEntity ver = (SetRefIngEmbIdEntity) getAuditReader().createQuery()
				.forEntitiesAtRevision( SetRefIngEmbIdEntity.class, 2 )
				.add( AuditEntity.property( "reference" ).isNull() )
				.getSingleResult();

		assert ver.getId().equals( idMulticolumnReferenceToParentNull );
	}

	@Test
	public void testReferencePropertyIsNotNullQuery() {
		CollectionRefIngEntity ver = (CollectionRefIngEntity) getAuditReader().createQuery()
				.forEntitiesAtRevision( CollectionRefIngEntity.class, 3 )
				.add( AuditEntity.property( "reference" ).isNotNull() )
				.getSingleResult();

		assert ver.getId().equals( idReferenceToParentNotNull );
	}
}