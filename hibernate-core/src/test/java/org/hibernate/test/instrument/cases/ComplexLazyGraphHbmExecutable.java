/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.instrument.cases;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.stat.Statistics;
import org.hibernate.test.instrument.domain.AEntity;
import org.hibernate.test.instrument.domain.BEntity;
import org.hibernate.test.instrument.domain.CEntity;
import org.hibernate.test.instrument.domain.DEntity;
import org.hibernate.test.instrument.domain.EEntity;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Blob;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.inTransaction;

/**
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-11223" )
public class ComplexLazyGraphHbmExecutable extends AbstractExecutable {
	@Override
	public void execute() throws Exception {
		prepareTestData();
		try {
			EntityPersister dEntityPersister = ((SessionFactoryImplementor) getFactory()).getEntityPersister(DEntity.class.getName());
			assertThat( dEntityPersister.getInstrumentationMetadata().isInstrumented(), is( true ) );

			loadAndNavigateSimple();
//			testNonOwningOneToOneAccess();
//			testOwningOneToOneAccess();
		}
		finally {
			cleanUpTestData();
		}
	}

	private void loadAndNavigateSimple() throws Exception {
		final Statistics stats = getFactory().getStatistics();
		stats.clear();

		inTransaction(
				getFactory(),
				new Consumer<Session>() {
					@Override
					public void accept(Session session) {
						try {
							final DEntity myD = (DEntity) session.load(DEntity.class, 1L);
							assertThat(stats.getPrepareStatementCount(), is(0L));

							System.out.println("Property-Value: " + myD.getD());
							System.out.println("######################################################");
							System.out.println("Association-Value: " + myD.getA().getA());
							System.out.println("######################################################");
							Blob lBlob = myD.getBlob();
							InputStream lIS = lBlob.getBinaryStream();
							ByteArrayOutputStream lBytesOut = new ByteArrayOutputStream();
							int len = 0;
							byte[] bytes = new byte[2000];
							while ((len = lIS.read(bytes)) > -1) {
								lBytesOut.write(bytes, 0, len);
							}
							lIS.close();
							lBytesOut.close();
							System.out.println("Blob-Value: " + lBytesOut.toString());
							System.out.println("######################################################");
							System.out.println(session.getTransaction().isActive());
							System.out.println("Association-Value: " + myD.getC().getC1() + " " + myD.getC().getC2());
							System.out.println("######################################################");
							Set<BEntity> lBs = myD.getBs();
							for (BEntity lB : lBs) {
								System.out.println(lB.getB1() + " " + lB.getB2());
							}
							System.out.println("EEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE");
							System.out.println("Association-Value (E): " + myD.getE().getE1() + " " + myD.getE().getE2());
							System.out.println("******************************************************");
						}
						catch (RuntimeException e) {
							throw e;
						}
						catch (Exception e) {
							throw new RuntimeException( e );
						}
					}
				}
		);
	}

	public void testNonOwningOneToOneAccess() {
		final Statistics stats = getFactory().getStatistics();
		stats.clear();

		inTransaction(
				getFactory(),
				new Consumer<Session>() {
					@Override
					public void accept(Session session) {
						final DEntity entityD = (DEntity) session.load(DEntity.class, 1L);
						// todo : really this should be 0
						assertThat( stats.getPrepareStatementCount(), is( 0L ) );

						entityD.getA();
						entityD.getC();

//						entityD.getC();
//						assertThat( stats.getPrepareStatementCount(), is( 2L ) );
//						entityD.getC();
//						assertThat( stats.getPrepareStatementCount(), is( 2L ) );
//
//						final EEntity e1 = entityD.getE();
//						assertThat( stats.getPrepareStatementCount(), is( 3L ) );
//
//						final EEntity e2 = entityD.getE();
//						assertThat( stats.getPrepareStatementCount(), is( 3L ) );
					}
				}
		);
	}

	@Test
	public void testOwningOneToOneAccess() {
		final Statistics stats = getFactory().getStatistics();
		stats.clear();

		inTransaction(
				getFactory(),
				new Consumer<Session>() {
					@Override
					public void accept(Session session) {
						final EEntity entityE = (EEntity) session.load(EEntity.class, 17L);
						assertThat( stats.getPrepareStatementCount(), is( 0L ) );

//						final DEntity d1 = entityE.getD();
//						assertThat( stats.getPrepareStatementCount(), is( 1 ) );
//
//						final DEntity d2 = entityE.getD();
//						assertThat( stats.getPrepareStatementCount(), is( 1 ) );
					}
				}
		);
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure(configuration);

		configuration.setProperty(AvailableSettings.GENERATE_STATISTICS, "true" );
		configuration.setProperty(AvailableSettings.USE_SECOND_LEVEL_CACHE, "false" );
		configuration.setProperty(AvailableSettings.USE_QUERY_CACHE, "false" );
	}

	@Override
	protected void applyMappings(Configuration cfg) {
		super.applyMappings(cfg);
		cfg.addResource( "org/hibernate/test/instrument/domain/ComplexLazyGraph.hbm.xml" );
	}

	private void prepareTestData() {
		inTransaction(
				getFactory(),
				new Consumer<Session>() {
					@Override
					public void accept(Session session) {
						DEntity d = new DEntity();
						d.setD("bla");
						d.setOid(1);

						byte[] lBytes = "agdfagdfagfgafgsfdgasfdgfgasdfgadsfgasfdgasfdgasdasfdg".getBytes();
						Blob lBlob = Hibernate.getLobCreator(session).createBlob(lBytes);
						d.setBlob(lBlob);

						BEntity b1 = new BEntity();
						b1.setOid(1);
						b1.setB1(34);
						b1.setB2("huhu");

						BEntity b2 = new BEntity();
						b2.setOid(2);
						b2.setB1(37);
						b2.setB2("haha");

						Set<BEntity> lBs = new HashSet<BEntity>();
						lBs.add(b1);
						lBs.add(b2);
						d.setBs(lBs);

						AEntity a = new AEntity();
						a.setOid(1);
						a.setA("hihi");
						d.setA(a);

						EEntity e = new EEntity();
						e.setOid(17);
						e.setE1("Balu");
						e.setE2("Bär");

						e.setD(d);
						d.setE(e);

						CEntity c = new CEntity();
						c.setOid(1);
						c.setC1("ast");
						c.setC2("qwert");
						c.setC3("yxcv");
						d.setC(c);

						session.save(b1);
						session.save(b2);
						session.save(a);
						session.save(c);
						session.save(d);
						session.save(e);
					}
				}
		);
	}

	private void cleanUpTestData() {

	}

}
