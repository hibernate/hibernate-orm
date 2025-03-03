/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.immutable;

import java.util.Iterator;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.Hibernate;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.jdbc.ClobJdbcType;

import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.hibernate.testing.orm.junit.DialectContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Gavin King
 */
public class ImmutableTest extends BaseSessionFactoryFunctionalTest {

	private static class TextAsMaterializedClobType extends AbstractSingleColumnStandardBasicType<String> {
		public final static TextAsMaterializedClobType INSTANCE = new TextAsMaterializedClobType();

		public TextAsMaterializedClobType() {
			super( ClobJdbcType.DEFAULT, StringJavaType.INSTANCE );
		}

		public String getName() {
			return "text";
		}
	}

	@Override
	protected void applyMetadataBuilder(MetadataBuilder metadataBuilder) {
		Dialect dialect = DialectContext.getDialect();
		if ( OracleDialect.class.isInstance( dialect ) ) {
			metadataBuilder.applyBasicType( TextAsMaterializedClobType.INSTANCE );
		}
	}

	@Override
	protected void applySettings(StandardServiceRegistryBuilder builer) {
		builer.applySetting( Environment.GENERATE_STATISTICS, "true" );
		builer.applySetting( Environment.STATEMENT_BATCH_SIZE, "0" );
	}

	@Override
	public String[] getOrmXmlFiles() {
		return new String[] { "org/hibernate/orm/test/immutable/ContractVariation.hbm.xml" };
	}

	@Test
	public void testChangeImmutableEntityProxyToModifiable() {
		Contract contract = new Contract( null, "gavin", "phone" );
		ContractVariation contractVariation1 = new ContractVariation( 1, contract );
		contractVariation1.setText( "expensive" );
		ContractVariation contractVariation2 = new ContractVariation( 2, contract );
		contractVariation2.setText( "more expensive" );

		clearCounts();

		inTransaction(
				s -> {
					s.persist( contract );
					assertTrue( s.isReadOnly( contract ) );
					assertTrue( s.isReadOnly( contractVariation1 ) );
					assertTrue( s.isReadOnly( contractVariation2 ) );
				}
		);

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		inSession(
				s -> {
					s.beginTransaction();
					try {
						Contract c = s.getReference( Contract.class, contract.getId() );
//						Contract c = (Contract) s.createCriteria(Contract.class).uniqueResult();
						assertTrue( s.isReadOnly( c ) );
						assertEquals( "gavin", c.getCustomerName() );
						assertEquals( 2, c.getVariations().size() );
						Iterator<ContractVariation> it = c.getVariations().iterator();
						ContractVariation cv1 = it.next();
						assertEquals( "expensive", cv1.getText() );
						ContractVariation cv2 = it.next();
						assertEquals( "more expensive", cv2.getText() );
						assertTrue( s.isReadOnly( cv1 ) );
						assertTrue( s.isReadOnly( cv2 ) );

						assertTrue( c instanceof HibernateProxy );
						s.setReadOnly( c, false );
					}
					catch (IllegalStateException ex) {
						// expected
					}
					finally {
						s.getTransaction().rollback();
					}
				}
		);

		inTransaction(
				s -> {
					s.remove( contract );

					assertAllContractAndVariationsAreDeleted( s );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	@Test
	public void testChangeImmutableEntityToModifiable() {
		Contract contract = new Contract( null, "gavin", "phone" );
		ContractVariation contractVariation1 = new ContractVariation( 1, contract );
		contractVariation1.setText( "expensive" );
		ContractVariation contractVariation2 = new ContractVariation( 2, contract );
		contractVariation2.setText( "more expensive" );

		clearCounts();

		inTransaction(
				s -> {
					s.persist( contract );
					assertTrue( s.isReadOnly( contract ) );
					assertTrue( s.isReadOnly( contractVariation1 ) );
					assertTrue( s.isReadOnly( contractVariation2 ) );
				}
		);

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		inSession(
				s -> {
					Contract c = s.getReference( Contract.class, contract.getId() );
					assertTrue( s.isReadOnly( c ) );
					assertEquals( "gavin", c.getCustomerName() );
					assertEquals( 2, c.getVariations().size() );
					Iterator<ContractVariation> it = c.getVariations().iterator();
					ContractVariation cv1 = it.next();
					assertEquals( "expensive", cv1.getText() );
					ContractVariation cv2 = it.next();
					assertEquals( "more expensive", cv2.getText() );
					assertTrue( s.isReadOnly( cv1 ) );
					assertTrue( s.isReadOnly( cv2 ) );

					try {
						assertTrue( c instanceof HibernateProxy );
						s.setReadOnly(
								( (HibernateProxy) c ).getHibernateLazyInitializer().getImplementation(),
								false
						);
					}
					catch (IllegalStateException ex) {
						// expected
					}
					finally {
						s.getTransaction().rollback();
					}
				}
		);


		inTransaction(
				s -> {
					s.remove( contract );

					assertAllContractAndVariationsAreDeleted( s );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	@Test
	public void testPersistImmutable() {
		Contract contract = new Contract( null, "gavin", "phone" );
		ContractVariation contractVariation1 = new ContractVariation( 1, contract );
		contractVariation1.setText( "expensive" );
		ContractVariation contractVariation2 = new ContractVariation( 2, contract );
		contractVariation2.setText( "more expensive" );

		clearCounts();

		inTransaction(
				s -> {
					s.persist( contract );
					assertTrue( s.isReadOnly( contract ) );
					assertTrue( s.isReadOnly( contractVariation1 ) );
					assertTrue( s.isReadOnly( contractVariation2 ) );
				}
		);

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					Contract c = getContract( s );
					assertTrue( s.isReadOnly( c ) );
					assertEquals( "gavin", c.getCustomerName() );
					assertEquals( 2, c.getVariations().size() );
					Iterator<ContractVariation> it = c.getVariations().iterator();
					ContractVariation cv1 = it.next();
					assertEquals( "expensive", cv1.getText() );
					ContractVariation cv2 = it.next();
					assertEquals( "more expensive", cv2.getText() );
					assertTrue( s.isReadOnly( cv1 ) );
					assertTrue( s.isReadOnly( cv2 ) );
					s.remove( c );

					assertAllContractAndVariationsAreDeleted( s );
				}
		);
		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	@Test
	public void testPersistUpdateImmutableInSameTransaction() {
		Contract contract = new Contract( null, "gavin", "phone" );
		ContractVariation contractVariation1 = new ContractVariation( 1, contract );
		contractVariation1.setText( "expensive" );
		ContractVariation contractVariation2 = new ContractVariation( 2, contract );
		contractVariation2.setText( "more expensive" );

		clearCounts();

		inTransaction(
				s -> {
					s.persist( contract );
					assertTrue( s.isReadOnly( contract ) );
					assertTrue( s.isReadOnly( contractVariation1 ) );
					assertTrue( s.isReadOnly( contractVariation2 ) );
					contract.setCustomerName( "gail" );

				}
		);

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					Contract c = getContract( s );
					assertTrue( s.isReadOnly( c ) );
					assertEquals( "gavin", c.getCustomerName() );
					assertEquals( 2, c.getVariations().size() );
					Iterator<ContractVariation> it = c.getVariations().iterator();
					ContractVariation cv1 = it.next();
					assertEquals( "expensive", cv1.getText() );
					ContractVariation cv2 = it.next();
					assertEquals( "more expensive", cv2.getText() );
					assertTrue( s.isReadOnly( cv1 ) );
					assertTrue( s.isReadOnly( cv2 ) );
					s.remove( c );

					assertAllContractAndVariationsAreDeleted( s );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	@Test
	public void testSaveImmutable() {
		Contract contract = new Contract( null, "gavin", "phone" );
		ContractVariation contractVariation1 = new ContractVariation( 1, contract );
		contractVariation1.setText( "expensive" );
		ContractVariation contractVariation2 = new ContractVariation( 2, contract );
		contractVariation2.setText( "more expensive" );

		clearCounts();

		inTransaction(
				s -> {
					s.persist( contract );
					assertTrue( s.isReadOnly( contract ) );
					assertTrue( s.isReadOnly( contractVariation1 ) );
					assertTrue( s.isReadOnly( contractVariation2 ) );
				}
		);

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					Contract c = getContract( s );
					assertTrue( s.isReadOnly( c ) );
					assertEquals( "gavin", c.getCustomerName() );
					assertEquals( 2, c.getVariations().size() );
					Iterator<ContractVariation> it = c.getVariations().iterator();
					ContractVariation cv1 = it.next();
					assertEquals( "expensive", cv1.getText() );
					ContractVariation cv2 = it.next();
					assertEquals( "more expensive", cv2.getText() );
					assertTrue( s.isReadOnly( cv1 ) );
					assertTrue( s.isReadOnly( cv2 ) );
					s.remove( c );

					assertAllContractAndVariationsAreDeleted( s );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	@Test
	public void testRefreshImmutable() {
		Contract contract = new Contract( null, "gavin", "phone" );
		ContractVariation contractVariation1 = new ContractVariation( 1, contract );
		contractVariation1.setText( "expensive" );
		ContractVariation contractVariation2 = new ContractVariation( 2, contract );
		contractVariation2.setText( "more expensive" );

		clearCounts();

		inTransaction(
				s -> {
					s.persist( contract );
					assertTrue( s.isReadOnly( contract ) );
					assertTrue( s.isReadOnly( contractVariation1 ) );
					assertTrue( s.isReadOnly( contractVariation2 ) );
				}
		);

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					Contract c = s.get(Contract.class, contract.getId());
					c.setCustomerName( "joe" );
					s.merge( c );
				}
		);

		assertInsertCount( 0 );
		assertUpdateCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					Contract c = s.get(Contract.class, contract.getId());
					assertTrue( s.isReadOnly( c ) );
					assertEquals( "gavin", c.getCustomerName() );
					assertEquals( 2, c.getVariations().size() );
					Iterator<ContractVariation> it = c.getVariations().iterator();
					ContractVariation cv1 = it.next();
					assertEquals( "expensive", cv1.getText() );
					ContractVariation cv2 = it.next();
					assertEquals( "more expensive", cv2.getText() );
					assertTrue( s.isReadOnly( cv1 ) );
					assertTrue( s.isReadOnly( cv2 ) );
				}
		);

		inTransaction(
				s -> {
					s.remove( contract );

					assertAllContractAndVariationsAreDeleted( s );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	@Test
	public void testImmutable() {
		Contract contract = new Contract( null, "gavin", "phone" );
		ContractVariation contractVariation1 = new ContractVariation( 1, contract );
		contractVariation1.setText( "expensive" );
		ContractVariation contractVariation2 = new ContractVariation( 2, contract );
		contractVariation2.setText( "more expensive" );

		clearCounts();

		inTransaction(
				s -> {
					s.persist( contract );
					assertTrue( s.isReadOnly( contract ) );
					assertTrue( s.isReadOnly( contractVariation1 ) );
					assertTrue( s.isReadOnly( contractVariation2 ) );
				}
		);

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		inSession(
				s -> {
					try {
						s.beginTransaction();
						Contract c = getContract( s );
						assertTrue( s.isReadOnly( c ) );
						c.setCustomerName( "foo bar" );
						ContractVariation cv1 = c.getVariations().iterator().next();
						cv1.setText( "blah blah" );
						assertTrue( s.isReadOnly( cv1 ) );
						assertFalse( s.contains( contractVariation2 ) );
						s.getTransaction().commit();
						assertTrue( s.isReadOnly( c ) );
						assertTrue( s.isReadOnly( cv1 ) );
						assertFalse( s.contains( contractVariation2 ) );
					}
					catch (Exception e) {
						if ( s.getTransaction().isActive() ) {
							s.getTransaction().rollback();
						}
						throw e;
					}
				}
		);


		assertInsertCount( 0 );
		assertUpdateCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					Contract c = getContract( s );
					assertTrue( s.isReadOnly( c ) );
					assertEquals( "gavin", c.getCustomerName() );
					assertEquals( 2, c.getVariations().size() );
					Iterator<ContractVariation> it = c.getVariations().iterator();
					ContractVariation cv1 = it.next();
					assertEquals( "expensive", cv1.getText() );
					ContractVariation cv2 = it.next();
					assertEquals( "more expensive", cv2.getText() );
					assertTrue( s.isReadOnly( cv1 ) );
					assertTrue( s.isReadOnly( cv2 ) );
					s.remove( c );

					assertAllContractAndVariationsAreDeleted( s );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	@Test
	public void testPersistAndUpdateImmutable() {
		Contract contract = new Contract( null, "gavin", "phone" );
		ContractVariation contractVariation1 = new ContractVariation( 1, contract );
		contractVariation1.setText( "expensive" );
		ContractVariation contractVariation2 = new ContractVariation( 2, contract );
		contractVariation2.setText( "more expensive" );

		clearCounts();

		inTransaction(
				s -> {
					s.persist( contract );
					assertTrue( s.isReadOnly( contract ) );
					assertTrue( s.isReadOnly( contractVariation1 ) );
					assertTrue( s.isReadOnly( contractVariation2 ) );
					contract.setCustomerName( "Sherman" );
				}
		);

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		inSession(
				s -> {
					try {
						s.beginTransaction();
						Contract c = getContract( s );
						assertTrue( s.isReadOnly( c ) );
						c.setCustomerName( "foo bar" );
						ContractVariation cv1 = c.getVariations().iterator().next();
						cv1.setText( "blah blah" );
						assertTrue( s.isReadOnly( cv1 ) );
						assertFalse( s.contains( contractVariation2 ) );
						s.getTransaction().commit();
						assertTrue( s.isReadOnly( c ) );
						assertTrue( s.isReadOnly( cv1 ) );
						assertFalse( s.contains( contractVariation2 ) );
					}
					catch (Exception e) {
						if ( s.getTransaction().isActive() ) {
							s.getTransaction().rollback();
						}
						throw e;
					}
				}
		);

		assertInsertCount( 0 );
		assertUpdateCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					Contract c = getContract( s );
					assertTrue( s.isReadOnly( c ) );
					assertEquals( "gavin", c.getCustomerName() );
					assertEquals( 2, c.getVariations().size() );
					Iterator<ContractVariation> it = c.getVariations().iterator();
					ContractVariation cv1 = it.next();
					assertEquals( "expensive", cv1.getText() );
					ContractVariation cv2 = it.next();
					assertEquals( "more expensive", cv2.getText() );
					assertTrue( s.isReadOnly( cv1 ) );
					assertTrue( s.isReadOnly( cv2 ) );
					s.remove( c );

					assertAllContractAndVariationsAreDeleted( s );

				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	@Test
	public void testUpdateAndDeleteManagedImmutable() {
		Contract contract = new Contract( null, "gavin", "phone" );
		ContractVariation contractVariation1 = new ContractVariation( 1, contract );
		contractVariation1.setText( "expensive" );
		ContractVariation contractVariation2 = new ContractVariation( 2, contract );
		contractVariation2.setText( "more expensive" );

		clearCounts();

		inTransaction( s -> s.persist( contract ) );

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					Contract c = getContract( s );
					assertTrue( s.isReadOnly( c ) );
					assertEquals( "gavin", c.getCustomerName() );
					assertEquals( 2, c.getVariations().size() );
					Iterator<ContractVariation> it = c.getVariations().iterator();
					ContractVariation cv1 = it.next();
					assertEquals( "expensive", cv1.getText() );
					ContractVariation cv2 = it.next();
					assertEquals( "more expensive", cv2.getText() );
					assertTrue( s.isReadOnly( cv1 ) );
					assertTrue( s.isReadOnly( cv2 ) );
					c.setCustomerName( "Sherman" );
					s.remove( c );

					assertAllContractAndVariationsAreDeleted( s );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	@Test
	public void testGetAndDeleteManagedImmutable() {
		Contract contract = new Contract( null, "gavin", "phone" );
		ContractVariation contractVariation1 = new ContractVariation( 1, contract );
		contractVariation1.setText( "expensive" );
		ContractVariation contractVariation2 = new ContractVariation( 2, contract );
		contractVariation2.setText( "more expensive" );

		clearCounts();

		inTransaction( s -> s.persist( contract ) );

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					Contract c = s.get( Contract.class, contract.getId() );
					assertTrue( s.isReadOnly( c ) );
					assertEquals( "gavin", c.getCustomerName() );
					assertEquals( 2, c.getVariations().size() );
					Iterator<ContractVariation> it = c.getVariations().iterator();
					ContractVariation cv1 = it.next();
					assertEquals( "expensive", cv1.getText() );
					ContractVariation cv2 = it.next();
					assertEquals( "more expensive", cv2.getText() );
					assertTrue( s.isReadOnly( cv1 ) );
					assertTrue( s.isReadOnly( cv2 ) );
					c.setCustomerName( "Sherman" );
					s.remove( c );

					assertAllContractAndVariationsAreDeleted( s );

				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	@Test
	public void testDeleteDetachedImmutable() {
		Contract contract = new Contract( null, "gavin", "phone" );
		ContractVariation contractVariation1 = new ContractVariation( 1, contract );
		contractVariation1.setText( "expensive" );
		ContractVariation contractVariation2 = new ContractVariation( 2, contract );
		contractVariation2.setText( "more expensive" );

		clearCounts();

		inTransaction( s -> s.persist( contract ) );

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					s.remove( contract );
					Contract c = getContract( s );
					assertNull( c );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	@Test
	public void testDeleteDetachedModifiedImmutable() {
		Contract contract = new Contract( null, "gavin", "phone" );
		ContractVariation contractVariation1 = new ContractVariation( 1, contract );
		contractVariation1.setText( "expensive" );
		ContractVariation contractVariation2 = new ContractVariation( 2, contract );
		contractVariation2.setText( "more expensive" );

		clearCounts();

		inTransaction( s -> s.persist( contract ) );

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					contract.setCustomerName( "sherman" );
					s.remove( contract );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}


	@Test
	public void testImmutableParentEntityWithUpdate() {
		Contract contract = new Contract( null, "gavin", "phone" );
		ContractVariation contractVariation1 = new ContractVariation( 1, contract );
		contractVariation1.setText( "expensive" );
		ContractVariation contractVariation2 = new ContractVariation( 2, contract );
		contractVariation2.setText( "more expensive" );

		clearCounts();

		inTransaction( s -> s.persist( contract ) );

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		inSession(
				s -> {
					try {
						s.beginTransaction();
						contract.setCustomerName( "foo bar" );
						Contract merged = s.merge( contract );
						assertTrue( s.isReadOnly( merged ) );
						for ( Iterator<ContractVariation> it = merged.getVariations().iterator(); it.hasNext(); ) {
							assertTrue( s.contains( it.next() ) );
						}
						s.getTransaction().commit();
						assertTrue( s.isReadOnly( merged ) );
						for ( Iterator<ContractVariation> it = merged.getVariations().iterator(); it.hasNext(); ) {
							ContractVariation cv = it.next();
							assertTrue( s.contains( cv ) );
							assertTrue( s.isReadOnly( cv ) );
						}
					}
					catch (Exception e) {
						if ( s.getTransaction().isActive() ) {
							s.getTransaction().rollback();
						}
						throw e;
					}
				}
		);

		assertUpdateCount( 0 );

		inTransaction(
				s -> {
					Contract c = getContract( s );
					assertEquals( "gavin", c.getCustomerName() );
					assertEquals( 2, c.getVariations().size() );
					Iterator<ContractVariation> it = c.getVariations().iterator();
					ContractVariation cv1 = it.next();
					assertEquals( "expensive", cv1.getText() );
					ContractVariation cv2 = it.next();
					assertEquals( "more expensive", cv2.getText() );
					s.remove( c );
					assertAllContractAndVariationsAreDeleted( s );

				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	@Test
	public void testImmutableChildEntityWithUpdate() {
		Contract contract = new Contract( null, "gavin", "phone" );
		ContractVariation contractVariation1 = new ContractVariation( 1, contract );
		contractVariation1.setText( "expensive" );
		ContractVariation contractVariation2 = new ContractVariation( 2, contract );
		contractVariation2.setText( "more expensive" );

		clearCounts();

		inTransaction( s -> s.persist( contract ) );

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		inSession(
				s -> {
					try {
						s.beginTransaction();
						ContractVariation cv1 = contract.getVariations().iterator().next();
						cv1.setText( "blah blah" );
						Contract merged = s.merge( contract );
						assertTrue( s.isReadOnly( merged ) );
						assertEquals( 2, merged.getVariations().size() );
						s.getTransaction().commit();
						assertTrue( s.isReadOnly( merged ) );
						assertTrue( s.isReadOnly( merged.getVariations().get( 0 ) ) );
						assertTrue( s.isReadOnly( merged.getVariations().get( 1 ) ) );
					}
					catch (Exception e) {
						if ( s.getTransaction().isActive() ) {
							s.getTransaction().rollback();
						}
						throw e;
					}
				}
		);

		assertUpdateCount( 0 );

		inTransaction(
				s -> {
					Contract c = getContract( s );
					assertEquals( "gavin", c.getCustomerName() );
					assertEquals( 2, c.getVariations().size() );
					Iterator<ContractVariation> it = c.getVariations().iterator();
					ContractVariation cv1 = it.next();
					assertEquals( "expensive", cv1.getText() );
					ContractVariation cv2 = it.next();
					assertEquals( "more expensive", cv2.getText() );
					s.remove( c );
					assertAllContractAndVariationsAreDeleted( s );

				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	@Test
	public void testImmutableCollectionWithUpdate() {
		Contract contract = new Contract( null, "gavin", "phone" );
		ContractVariation contractVariation1 = new ContractVariation( 1, contract );
		contractVariation1.setText( "expensive" );
		ContractVariation contractVariation2 = new ContractVariation( 2, contract );
		contractVariation2.setText( "more expensive" );

		clearCounts();

		inTransaction( s -> s.persist( contract ) );

		assertInsertCount( 3 );
		assertUpdateCount( 0 );

		inSession(
				s -> {
					s.beginTransaction();
					contract.getVariations().add( new ContractVariation( 3, contract ) );
					try {
						s.merge( contract );
						s.getTransaction().commit();
						fail( "should have failed because reassociated object has a dirty collection" );
					}
					catch (PersistenceException ex) {
						// expected
					}
					finally {
						if ( s.getTransaction().isActive() ) {
							s.getTransaction().rollback();
						}
					}
				}
		);


		assertUpdateCount( 0 );

		inTransaction(
				s -> {
					Contract c = getContract( s );
					assertEquals( "gavin", c.getCustomerName() );
					assertEquals( 2, c.getVariations().size() );
					Iterator<ContractVariation> it = c.getVariations().iterator();
					ContractVariation cv1 = it.next();
					assertEquals( "expensive", cv1.getText() );
					ContractVariation cv2 = it.next();
					assertEquals( "more expensive", cv2.getText() );
					s.remove( c );

					assertAllContractAndVariationsAreDeleted( s );

				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	@Test
	public void testUnmodifiedImmutableParentEntityWithMerge() {
		Contract contract = new Contract( null, "gavin", "phone" );
		ContractVariation contractVariation1 = new ContractVariation( 1, contract );
		contractVariation1.setText( "expensive" );
		ContractVariation contractVariation2 = new ContractVariation( 2, contract );
		contractVariation2.setText( "more expensive" );

		clearCounts();

		inTransaction( s -> s.persist( contract ) );

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					Contract c = (Contract) s.merge( contract );
					assertTrue( s.isReadOnly( c ) );
					assertTrue( Hibernate.isInitialized( c.getVariations() ) );
					Iterator<ContractVariation> it = c.getVariations().iterator();
					ContractVariation cv1 = it.next();
					ContractVariation cv2 = it.next();
					assertTrue( s.isReadOnly( cv1 ) );
					assertTrue( s.isReadOnly( cv2 ) );
				}
		);


		assertUpdateCount( 0 );

		inTransaction(
				s -> {
					Contract c = getContract( s );
					assertEquals( "gavin", c.getCustomerName() );
					assertEquals( 2, c.getVariations().size() );
					Iterator<ContractVariation> it = c.getVariations().iterator();
					ContractVariation cv1 = it.next();
					assertEquals( "expensive", cv1.getText() );
					ContractVariation cv2 = it.next();
					assertEquals( "more expensive", cv2.getText() );
					s.remove( c );
					assertAllContractAndVariationsAreDeleted( s );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	@Test
	public void testImmutableParentEntityWithMerge() {
		Contract contract = new Contract( null, "gavin", "phone" );
		ContractVariation contractVariation1 = new ContractVariation( 1, contract );
		contractVariation1.setText( "expensive" );
		ContractVariation contractVariation2 = new ContractVariation( 2, contract );
		contractVariation2.setText( "more expensive" );

		clearCounts();

		inTransaction( s -> s.persist( contract ) );

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					contract.setCustomerName( "foo bar" );
					Contract c = (Contract) s.merge( contract );
					assertTrue( s.isReadOnly( c ) );
					assertTrue( Hibernate.isInitialized( c.getVariations() ) );
					Iterator<ContractVariation> it = c.getVariations().iterator();
					ContractVariation cv1 = it.next();
					ContractVariation cv2 = it.next();
					assertTrue( s.isReadOnly( c ) );
					assertTrue( s.isReadOnly( c ) );
				}
		);

		assertUpdateCount( 0 );

		inTransaction(
				s -> {
					Contract c = getContract( s );
					assertEquals( "gavin", c.getCustomerName() );
					assertEquals( 2, c.getVariations().size() );
					Iterator<ContractVariation> it = c.getVariations().iterator();
					ContractVariation cv1 = it.next();
					assertEquals( "expensive", cv1.getText() );
					ContractVariation cv2 = it.next();
					assertEquals( "more expensive", cv2.getText() );
					s.remove( c );
					assertAllContractAndVariationsAreDeleted( s );

				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	@Test
	public void testImmutableChildEntityWithMerge() {
		Contract contract = new Contract( null, "gavin", "phone" );
		ContractVariation contractVariation1 = new ContractVariation( 1, contract );
		contractVariation1.setText( "expensive" );
		ContractVariation contractVariation2 = new ContractVariation( 2, contract );
		contractVariation2.setText( "more expensive" );

		clearCounts();

		inTransaction( s -> s.persist( contract ) );

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					ContractVariation cv1 = contract.getVariations().iterator().next();
					cv1.setText( "blah blah" );
					Contract c = (Contract) s.merge( contract );
					assertTrue( s.isReadOnly( c ) );
					assertTrue( Hibernate.isInitialized( c.getVariations() ) );
					Iterator<ContractVariation> it = c.getVariations().iterator();
					cv1 = it.next();
					ContractVariation cv2 = it.next();
					assertTrue( s.isReadOnly( c ) );
					assertTrue( s.isReadOnly( c ) );
				}
		);

		assertUpdateCount( 0 );

		inTransaction(
				s -> {
					Contract c = getContract( s );
					assertEquals( "gavin", c.getCustomerName() );
					assertEquals( 2, c.getVariations().size() );
					Iterator<ContractVariation> it = c.getVariations().iterator();
					ContractVariation cv1 = it.next();
					assertEquals( "expensive", cv1.getText() );
					ContractVariation cv2 = it.next();
					assertEquals( "more expensive", cv2.getText() );
					s.remove( c );
					assertAllContractAndVariationsAreDeleted( s );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	@Test
	public void testImmutableCollectionWithMerge() {
		Contract contract = new Contract( null, "gavin", "phone" );
		ContractVariation contractVariation1 = new ContractVariation( 1, contract );
		contractVariation1.setText( "expensive" );
		ContractVariation contractVariation2 = new ContractVariation( 2, contract );
		contractVariation2.setText( "more expensive" );

		clearCounts();

		inTransaction( s -> s.persist( contract ) );

		assertInsertCount( 3 );
		assertUpdateCount( 0 );

		clearCounts();

		inSession(
				s -> {
					s.beginTransaction();
					contract.getVariations().add( new ContractVariation( 3, contract ) );
					s.merge( contract );
					try {
						s.getTransaction().commit();
						fail( "should have failed because an immutable collection was changed" );
					}
					catch (PersistenceException ex) {
						// expected
						s.getTransaction().rollback();
					}
				}
		);

		inTransaction(
				s -> {
					Contract c = getContract( s );
					assertEquals( "gavin", c.getCustomerName() );
					assertEquals( 2, c.getVariations().size() );
					Iterator<ContractVariation> it = c.getVariations().iterator();
					ContractVariation cv1 = it.next();
					assertEquals( "expensive", cv1.getText() );
					ContractVariation cv2 = it.next();
					assertEquals( "more expensive", cv2.getText() );
					s.remove( c );
					assertAllContractAndVariationsAreDeleted( s );

				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	@Test
	public void testNewEntityViaImmutableEntityWithImmutableCollectionUsingMerge() {
		Contract contract = new Contract( null, "gavin", "phone" );
		ContractVariation contractVariation1 = new ContractVariation( 1, contract );
		contractVariation1.setText( "expensive" );
		ContractVariation contractVariation2 = new ContractVariation( 2, contract );
		contractVariation2.setText( "more expensive" );

		clearCounts();

		inTransaction( s -> s.persist( contract ) );

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					contractVariation1.getInfos().add( new Info( "cv1 info" ) );
					s.merge( contract );
				}
		);

		assertInsertCount( 1 );
		assertUpdateCount( 0 );

		inTransaction(
				s -> {
					Contract c = getContract( s );
					assertEquals( "gavin", c.getCustomerName() );
					assertEquals( 2, c.getVariations().size() );
					Iterator<ContractVariation> it = c.getVariations().iterator();
					ContractVariation cv1 = it.next();
					assertEquals( "expensive", cv1.getText() );
					assertEquals( 1, cv1.getInfos().size() );
					assertEquals( "cv1 info", ( (Info) cv1.getInfos().iterator().next() ).getText() );
					ContractVariation cv2 = it.next();
					assertEquals( "more expensive", cv2.getText() );
					s.remove( c );
					assertAllContractAndVariationsAreDeleted( s );

				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 4 );
	}

	@Test
	public void testUpdatedEntityViaImmutableEntityWithImmutableCollectionUsingMerge() {
		clearCounts();

		Contract contract = new Contract( null, "gavin", "phone" );
		ContractVariation contractVariation1 = new ContractVariation( 1, contract );
		contractVariation1.setText( "expensive" );
		Info cv1Info = new Info( "cv1 info" );
		contractVariation1.getInfos().add( cv1Info );
		ContractVariation contractVariation2 = new ContractVariation( 2, contract );
		contractVariation2.setText( "more expensive" );

		inTransaction( s -> s.persist( contract ) );

		assertInsertCount( 4 );
		assertUpdateCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					cv1Info.setText( "new cv1 info" );
					s.merge( contract );
				}
		);

		assertInsertCount( 0 );
		assertUpdateCount( 1 );
		clearCounts();

		inTransaction(
				s -> {
					Contract c = getContract( s );
					assertEquals( "gavin", c.getCustomerName() );
					assertEquals( 2, c.getVariations().size() );
					Iterator<ContractVariation> it = c.getVariations().iterator();
					ContractVariation cv1 = it.next();
					assertEquals( "expensive", cv1.getText() );
					assertEquals( 1, cv1.getInfos().size() );
					assertEquals( "new cv1 info", ( (Info) cv1.getInfos().iterator().next() ).getText() );
					ContractVariation cv2 = it.next();
					assertEquals( "more expensive", cv2.getText() );
					s.remove( c );
					assertAllContractAndVariationsAreDeleted( s );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 4 );
	}

	@Test
	public void testImmutableEntityAddImmutableToInverseMutableCollection() {
		clearCounts();

		Contract contract = new Contract( null, "gavin", "phone" );
		ContractVariation contractVariation1 = new ContractVariation( 1, contract );
		contractVariation1.setText( "expensive" );
		ContractVariation contractVariation2 = new ContractVariation( 2, contract );
		contractVariation2.setText( "more expensive" );
		Party party = new Party( "a party" );

		inTransaction(
				s -> {
					s.persist( contract );
					s.persist( party );
				}
		);

		assertInsertCount( 4 );
		assertUpdateCount( 0 );
		clearCounts();

		Contract merged = fromTransaction(
				s -> {
					contract.addParty( new Party( "a new party" ) );
					return s.merge( contract );
				}
		);

		assertInsertCount( 1 );
		assertUpdateCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					merged.addParty( party );
					s.merge( merged );
				}
		);

		inTransaction(
				s -> {
					Contract c = getContract( s );
					assertEquals( "gavin", c.getCustomerName() );
					assertEquals( 2, c.getVariations().size() );
					Iterator<ContractVariation> it = c.getVariations().iterator();
					ContractVariation cv1 = it.next();
					assertEquals( "expensive", cv1.getText() );
					ContractVariation cv2 = it.next();
					assertEquals( "more expensive", cv2.getText() );
					//assertEquals( 2, c.getParties().size() );
					s.remove( c );

					assertAllContractAndVariationsAreDeleted( s );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 4 );
	}

	@Test
	public void testImmutableEntityRemoveImmutableFromInverseMutableCollection() {
		clearCounts();

		Contract contract = new Contract( null, "gavin", "phone" );
		ContractVariation contractVariation1 = new ContractVariation( 1, contract );
		contractVariation1.setText( "expensive" );
		ContractVariation contractVariation2 = new ContractVariation( 2, contract );
		contractVariation2.setText( "more expensive" );
		Party party = new Party( "party1" );
		contract.addParty( party );

		inTransaction( s -> s.persist( contract ) );

		assertInsertCount( 4 );
		assertUpdateCount( 0 );
		clearCounts();

		party = (Party) contract.getParties().iterator().next();
		contract.removeParty( party );

		inTransaction( s -> s.merge( contract ) );

		assertUpdateCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					Contract c = getContract( s );
					assertEquals( "gavin", c.getCustomerName() );
					assertEquals( 2, c.getVariations().size() );
					Iterator<ContractVariation> it = c.getVariations().iterator();
					ContractVariation cv1 = it.next();
					assertEquals( "expensive", cv1.getText() );
					ContractVariation cv2 = it.next();
					assertEquals( "more expensive", cv2.getText() );
					//assertEquals( 0, c.getParties().size() );
					s.remove( c );

					assertAllContractAndVariationsAreDeleted( s );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 4 );
	}

	@Test
	public void testImmutableEntityRemoveImmutableFromInverseMutableCollectionByDelete() {
		clearCounts();

		Contract contract = new Contract( null, "gavin", "phone" );
		ContractVariation contractVariation1 = new ContractVariation( 1, contract );
		contractVariation1.setText( "expensive" );
		ContractVariation contractVariation2 = new ContractVariation( 2, contract );
		contractVariation2.setText( "more expensive" );
		Party p = new Party( "party1" );
		contract.addParty( p );

		inTransaction( s -> s.persist( contract ) );


		assertInsertCount( 4 );
		assertUpdateCount( 0 );
		clearCounts();

		Party party = (Party) contract.getParties().iterator().next();

		inSession(
				s -> {
					try {
						s.beginTransaction();
						s.remove( party );
						s.getTransaction().commit();
					}
					catch (Exception e) {
						if ( s.getTransaction().isActive() ) {
							s.getTransaction().rollback();
						}
						throw e;
					}
				}
		);


		assertUpdateCount( 0 );
		assertDeleteCount( 1 );
		clearCounts();

		inTransaction(
				s -> {
					Contract c = getContract( s );
					assertEquals( "gavin", c.getCustomerName() );
					assertEquals( 2, c.getVariations().size() );
					Iterator<ContractVariation> it = c.getVariations().iterator();
					ContractVariation cv1 = it.next();
					assertEquals( "expensive", cv1.getText() );
					ContractVariation cv2 = it.next();
					assertEquals( "more expensive", cv2.getText() );
					assertEquals( 0, c.getParties().size() );
					s.remove( c );
					assertAllContractAndVariationsAreDeleted( s );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	@Test
	public void testImmutableEntityRemoveImmutableFromInverseMutableCollectionByDeref() {
		clearCounts();

		Contract contract = new Contract( null, "gavin", "phone" );
		ContractVariation contractVariation1 = new ContractVariation( 1, contract );
		contractVariation1.setText( "expensive" );
		ContractVariation contractVariation2 = new ContractVariation( 2, contract );
		contractVariation2.setText( "more expensive" );
		final Party party = new Party( "party1" );
		contract.addParty( party );

		inTransaction( s -> s.persist( contract ) );

		assertInsertCount( 4 );
		assertUpdateCount( 0 );
		clearCounts();

		Party p = (Party) contract.getParties().iterator().next();
		party.setContract( null );

		inSession( s -> {
			try {
				s.beginTransaction();
				s.merge( p );
				s.getTransaction().commit();
			}
			catch (Exception e) {
				if ( s.getTransaction().isActive() ) {
					s.getTransaction().rollback();
				}
				throw e;
			}
		} );

		inTransaction(
				s -> {
					Party p1 = s.get( Party.class, party.getId() );
					assertNotNull( p1.getContract() );
				}
		);

		assertUpdateCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					Contract c = getContract( s );
					assertEquals( "gavin", c.getCustomerName() );
					assertEquals( 2, c.getVariations().size() );
					Iterator<ContractVariation> it = c.getVariations().iterator();
					ContractVariation cv1 = it.next();
					assertEquals( "expensive", cv1.getText() );
					ContractVariation cv2 = it.next();
					assertEquals( "more expensive", cv2.getText() );
					assertEquals( 1, c.getParties().size() );
					Party p1 = (Party) c.getParties().iterator().next();
					assertEquals( "party1", p1.getName() );
					assertSame( c, p1.getContract() );
					s.remove( c );

					assertAllContractAndVariationsAreDeleted( s );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 4 );
	}

	protected void clearCounts() {
		sessionFactory().getStatistics().clear();
	}

	protected void assertInsertCount(int expected) {
		int inserts = (int) sessionFactory().getStatistics().getEntityInsertCount();
		assertEquals( expected, inserts, "unexpected insert count" );
	}

	protected void assertUpdateCount(int expected) {
		int updates = (int) sessionFactory().getStatistics().getEntityUpdateCount();
		assertEquals( expected, updates, "unexpected update counts" );
	}

	protected void assertDeleteCount(int expected) {
		int deletes = (int) sessionFactory().getStatistics().getEntityDeleteCount();
		assertEquals( expected, deletes, "unexpected delete counts" );
	}

	private Long getContractRowCount(SessionImplementor s) {
		//	s.createCriteria( Contract.class ).setProjection( Projections.rowCount() ).uniqueResult(),
		CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
		CriteriaQuery<Long> criteria = criteriaBuilder.createQuery( Long.class );
		Root<Contract> contractRoot = criteria.from( Contract.class );
		criteria.select( criteriaBuilder.count( contractRoot ) );
		return s.createQuery( criteria ).uniqueResult();
	}

	private Long getContractVariationRowCount(SessionImplementor s) {
		//	s.createCriteria( ContractVariation.class ).setProjection( Projections.rowCount() ).uniqueResult(),
		CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
		CriteriaQuery<Long> criteria = criteriaBuilder.createQuery( Long.class );
		Root<ContractVariation> contractRoot = criteria.from( ContractVariation.class );
		criteria.select( criteriaBuilder.count( contractRoot ) );
		return s.createQuery( criteria ).uniqueResult();
	}

	private Contract getContract(SessionImplementor s) {
		CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
		CriteriaQuery<Contract> criteria = criteriaBuilder.createQuery( Contract.class );
		criteria.from( Contract.class );
		return s.createQuery( criteria ).uniqueResult();
	}


	private void assertAllContractAndVariationsAreDeleted(SessionImplementor s) {
		assertEquals( getContractRowCount( s ), new Long( 0 ) );
		assertEquals( getContractVariationRowCount( s ), new Long( 0 ) );
	}

}
