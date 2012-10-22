package org.hibernate.test.annotations.enumerated;

import java.io.Serializable;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.HibernateTypeDescriptor;
import org.hibernate.test.annotations.enumerated.EntityEnum.Common;
import org.hibernate.test.annotations.enumerated.EntityEnum.FirstLetter;
import org.hibernate.test.annotations.enumerated.EntityEnum.LastNumber;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.type.EnumType;

import static org.junit.Assert.assertEquals;

/**
 * Test type definition for enum
 * 
 * @author Janario Oliveira
 */
public class EnumeratedTypeTest extends BaseCoreFunctionalTestCase {

	@Test
	public void testTypeDefinition() {
		EntityBinding binding = getEntityBinding( EntityEnum.class );

		// ordinal default of EnumType
		HibernateTypeDescriptor ordinalEnum = binding
				.locateAttributeBinding( "ordinal" )
				.getHibernateTypeDescriptor();
		assertEquals( Common.class, ordinalEnum.getResolvedTypeMapping()
				.getReturnedClass() );
		assertEquals( EnumType.class.getName(), ordinalEnum.getExplicitTypeName() );

		// string defined by Enumerated(STRING)
		HibernateTypeDescriptor stringEnum = binding
				.locateAttributeBinding( "string" )
				.getHibernateTypeDescriptor();
		assertEquals( Common.class, stringEnum.getResolvedTypeMapping()
				.getReturnedClass() );
		assertEquals( EnumType.class.getName(), stringEnum.getExplicitTypeName() );

		// explicit defined by @Type
		HibernateTypeDescriptor first = binding
				.locateAttributeBinding( "firstLetter" )
				.getHibernateTypeDescriptor();
		assertEquals( FirstLetter.class, first.getResolvedTypeMapping()
				.getReturnedClass() );
		assertEquals( FirstLetterType.class.getName(), first.getExplicitTypeName() );

		// implicit defined by @TypeDef in somewhere
		HibernateTypeDescriptor last = binding
				.locateAttributeBinding( "lastNumber" )
				.getHibernateTypeDescriptor();
		assertEquals( LastNumber.class, last.getResolvedTypeMapping()
				.getReturnedClass() );
		assertEquals( LastNumberType.class.getName(), last.getExplicitTypeName() );

		// implicit defined by @TypeDef in anywhere, but overrided by Enumerated(STRING)
		HibernateTypeDescriptor implicitOverrideExplicit = binding
				.locateAttributeBinding( "explicitOverridingImplicit" )
				.getHibernateTypeDescriptor();
		assertEquals( LastNumber.class,
				implicitOverrideExplicit.getResolvedTypeMapping()
						.getReturnedClass() );
		assertEquals( EnumType.class.getName(), 
				implicitOverrideExplicit.getExplicitTypeName() );
	}

	@Test
	public void testTypeQuery() {
		Session session = openSession();
		session.getTransaction().begin();

		// persist
		EntityEnum entityEnum = new EntityEnum();
		entityEnum.setOrdinal( Common.A2 );
		Serializable id = session.save( entityEnum );

		session.getTransaction().commit();
		session.close();
		session = openSession();
		session.getTransaction().begin();

		// find
		entityEnum = (EntityEnum) session.createQuery( "from EntityEnum ee where ee.ordinal=1" ).uniqueResult();
		assertEquals( id, entityEnum.getId() );
		assertEquals( Common.A2, entityEnum.getOrdinal() );
		// find parameter
		entityEnum = (EntityEnum) session.createQuery( "from EntityEnum ee where ee.ordinal=:ordinal" )
				.setParameter( "ordinal", Common.A2 ).uniqueResult();
		assertEquals( id, entityEnum.getId() );
		assertEquals( Common.A2, entityEnum.getOrdinal() );
		// delete
		assertEquals( 1, session.createSQLQuery( "DELETE FROM EntityEnum where ordinal=1" ).executeUpdate() );

		session.getTransaction().commit();
		session.close();

		// **************
		session = openSession();
		session.getTransaction().begin();

		// persist
		entityEnum = new EntityEnum();
		entityEnum.setString( Common.B1 );
		id = session.save( entityEnum );

		session.getTransaction().commit();
		session.close();
		session = openSession();
		session.getTransaction().begin();

		// find
		entityEnum = (EntityEnum) session.createQuery( "from EntityEnum ee where ee.string='B1'" ).uniqueResult();
		assertEquals( id, entityEnum.getId() );
		assertEquals( Common.B1, entityEnum.getString() );
		// find parameter
		entityEnum = (EntityEnum) session.createQuery( "from EntityEnum ee where ee.string=:string" )
				.setParameter( "string", Common.B1 ).uniqueResult();
		assertEquals( id, entityEnum.getId() );
		assertEquals( Common.B1, entityEnum.getString() );
		// delete
		assertEquals( 1, session.createSQLQuery( "DELETE FROM EntityEnum where string='B1'" ).executeUpdate() );
		session.getTransaction().commit();
		session.close();

		// **************
		session = openSession();
		session.getTransaction().begin();

		// persist
		entityEnum = new EntityEnum();
		entityEnum.setFirstLetter( FirstLetter.C_LETTER );
		id = session.save( entityEnum );

		session.getTransaction().commit();
		session.close();
		session = openSession();
		session.getTransaction().begin();

		// find
		entityEnum = (EntityEnum) session.createQuery( "from EntityEnum ee where ee.firstLetter='C'" ).uniqueResult();
		assertEquals( id, entityEnum.getId() );
		assertEquals( FirstLetter.C_LETTER, entityEnum.getFirstLetter() );
		// find parameter
		entityEnum = (EntityEnum) session.createQuery( "from EntityEnum ee where ee.firstLetter=:firstLetter" )
				.setParameter( "firstLetter", FirstLetter.C_LETTER ).uniqueResult();
		assertEquals( id, entityEnum.getId() );
		assertEquals( FirstLetter.C_LETTER, entityEnum.getFirstLetter() );
		// delete
		assertEquals( 1, session.createSQLQuery( "DELETE FROM EntityEnum where firstLetter='C'" ).executeUpdate() );

		session.getTransaction().commit();
		session.close();

		// **************
		session = openSession();
		session.getTransaction().begin();

		// persist
		entityEnum = new EntityEnum();
		entityEnum.setLastNumber( LastNumber.NUMBER_1 );
		id = session.save( entityEnum );

		session.getTransaction().commit();
		session.close();
		session = openSession();
		session.getTransaction().begin();

		// find
		entityEnum = (EntityEnum) session.createQuery( "from EntityEnum ee where ee.lastNumber='1'" ).uniqueResult();
		assertEquals( id, entityEnum.getId() );
		assertEquals( LastNumber.NUMBER_1, entityEnum.getLastNumber() );
		// find parameter
		entityEnum = (EntityEnum) session.createQuery( "from EntityEnum ee where ee.lastNumber=:lastNumber" )
				.setParameter( "lastNumber", LastNumber.NUMBER_1 ).uniqueResult();
		assertEquals( id, entityEnum.getId() );
		assertEquals( LastNumber.NUMBER_1, entityEnum.getLastNumber() );
		// delete
		assertEquals( 1, session.createSQLQuery( "DELETE FROM EntityEnum where lastNumber='1'" ).executeUpdate() );

		session.getTransaction().commit();
		session.close();

		// **************
		session = openSession();
		session.getTransaction().begin();

		// persist
		entityEnum = new EntityEnum();
		entityEnum.setExplicitOverridingImplicit( LastNumber.NUMBER_2 );
		id = session.save( entityEnum );

		session.getTransaction().commit();
		session.close();
		session = openSession();
		session.getTransaction().begin();

		// find
		entityEnum = (EntityEnum) session.createQuery(
				"from EntityEnum ee where ee.explicitOverridingImplicit='NUMBER_2'" ).uniqueResult();
		assertEquals( id, entityEnum.getId() );
		assertEquals( LastNumber.NUMBER_2, entityEnum.getExplicitOverridingImplicit() );
		// find parameter
		entityEnum = (EntityEnum) session
				.createQuery( "from EntityEnum ee where ee.explicitOverridingImplicit=:override" )
				.setParameter( "override", LastNumber.NUMBER_2 ).uniqueResult();
		assertEquals( id, entityEnum.getId() );
		assertEquals( LastNumber.NUMBER_2, entityEnum.getExplicitOverridingImplicit() );
		// delete
		assertEquals( 1, session.createSQLQuery( "DELETE FROM EntityEnum where explicitOverridingImplicit='NUMBER_2'" )
				.executeUpdate() );

		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testTypeCriteria() {
		Session session = openSession();
		session.getTransaction().begin();

		// persist
		EntityEnum entityEnum = new EntityEnum();
		entityEnum.setOrdinal( Common.A1 );
		Serializable id = session.save( entityEnum );

		session.getTransaction().commit();
		session.close();
		session = openSession();
		session.getTransaction().begin();

		// find
		entityEnum = (EntityEnum) session.createCriteria( EntityEnum.class )
				.add( Restrictions.eq( "ordinal", Common.A1 ) ).uniqueResult();
		assertEquals( id, entityEnum.getId() );
		assertEquals( Common.A1, entityEnum.getOrdinal() );
		// delete
		assertEquals( 1, session.createSQLQuery( "DELETE FROM EntityEnum where ordinal=0" ).executeUpdate() );

		session.getTransaction().commit();
		session.close();

		// **************
		session = openSession();
		session.getTransaction().begin();

		// persist
		entityEnum = new EntityEnum();
		entityEnum.setString( Common.B2 );
		id = session.save( entityEnum );

		session.getTransaction().commit();
		session.close();
		session = openSession();
		session.getTransaction().begin();

		// find
		entityEnum = (EntityEnum) session.createCriteria( EntityEnum.class )
				.add( Restrictions.eq( "string", Common.B2 ) ).uniqueResult();
		assertEquals( id, entityEnum.getId() );
		assertEquals( Common.B2, entityEnum.getString() );
		// delete
		assertEquals( 1, session.createSQLQuery( "DELETE FROM EntityEnum where string='B2'" ).executeUpdate() );

		session.getTransaction().commit();
		session.close();

		// **************
		session = openSession();
		session.getTransaction().begin();

		// persist
		entityEnum = new EntityEnum();
		entityEnum.setFirstLetter( FirstLetter.A_LETTER );
		id = session.save( entityEnum );

		session.getTransaction().commit();
		session.close();
		session = openSession();
		session.getTransaction().begin();

		// find
		entityEnum = (EntityEnum) session.createCriteria( EntityEnum.class )
				.add( Restrictions.eq( "firstLetter", FirstLetter.A_LETTER ) ).uniqueResult();
		assertEquals( id, entityEnum.getId() );
		assertEquals( FirstLetter.A_LETTER, entityEnum.getFirstLetter() );
		// delete
		assertEquals( 1, session.createSQLQuery( "DELETE FROM EntityEnum where firstLetter='A'" ).executeUpdate() );

		session.getTransaction().commit();
		session.close();

		// **************
		session = openSession();
		session.getTransaction().begin();

		// persist
		entityEnum = new EntityEnum();
		entityEnum.setLastNumber( LastNumber.NUMBER_3 );
		id = session.save( entityEnum );

		session.getTransaction().commit();
		session.close();
		session = openSession();
		session.getTransaction().begin();

		// find
		entityEnum = (EntityEnum) session.createCriteria( EntityEnum.class )
				.add( Restrictions.eq( "lastNumber", LastNumber.NUMBER_3 ) ).uniqueResult();
		assertEquals( id, entityEnum.getId() );
		assertEquals( LastNumber.NUMBER_3, entityEnum.getLastNumber() );
		// delete
		assertEquals( 1, session.createSQLQuery( "DELETE FROM EntityEnum where lastNumber='3'" ).executeUpdate() );

		session.getTransaction().commit();
		session.close();

		// **************
		session = openSession();
		session.getTransaction().begin();

		// persist
		entityEnum = new EntityEnum();
		entityEnum.setExplicitOverridingImplicit( LastNumber.NUMBER_2 );
		id = session.save( entityEnum );

		session.getTransaction().commit();
		session.close();
		session = openSession();
		session.getTransaction().begin();

		// find
		entityEnum = (EntityEnum) session.createCriteria( EntityEnum.class )
				.add( Restrictions.eq( "explicitOverridingImplicit", LastNumber.NUMBER_2 ) ).uniqueResult();
		assertEquals( id, entityEnum.getId() );
		assertEquals( LastNumber.NUMBER_2, entityEnum.getExplicitOverridingImplicit() );
		// delete
		assertEquals( 1, session.createSQLQuery( "DELETE FROM EntityEnum where explicitOverridingImplicit='NUMBER_2'" )
				.executeUpdate() );

		session.getTransaction().commit();
		session.close();

	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { EntityEnum.class };
	}
}
