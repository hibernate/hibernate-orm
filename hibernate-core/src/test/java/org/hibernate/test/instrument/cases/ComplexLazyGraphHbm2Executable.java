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
import org.hibernate.test.instrument.domain.AEntity2;
import org.hibernate.test.instrument.domain.BEntity2;
import org.hibernate.test.instrument.domain.CEntity2;
import org.hibernate.test.instrument.domain.DEntity2;
import org.hibernate.test.instrument.domain.EEntity2;
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
public class ComplexLazyGraphHbm2Executable extends AbstractExecutable {
	@Override
	public void execute() throws Exception {
		prepareTestData();
		try {
			EntityPersister dEntityPersister = ((SessionFactoryImplementor) getFactory()).getEntityPersister(DEntity2.class.getName());
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
							final DEntity2 myD = (DEntity2) session.load(DEntity2.class, 1L);
							assertThat(stats.getPrepareStatementCount(), is(0L));

							System.out.println("Property-Value: " + myD.getD());
							System.out.println("######################################################");
							System.out.println("Association-Value: " + myD.getA().getA());
							System.out.println("######################################################");
							Blob lBlob = myD.getBlob();
							InputStream lIS = lBlob.getBinaryStream();
							ByteArrayOutputStream lBytesOut = new ByteArrayOutputStream();
							int len;
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
							Set<BEntity2> lBs = myD.getBs();
							for (BEntity2 lB : lBs) {
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
						final DEntity2 entityD = (DEntity2) session.load(DEntity2.class, 1L);
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
						final EEntity2 entityE = (EEntity2) session.load(EEntity2.class, 17L);
						assertThat( stats.getPrepareStatementCount(), is( 0L ) );

//						final DEntity2 d1 = entityE.getD();
//						assertThat( stats.getPrepareStatementCount(), is( 1 ) );
//
//						final DEntity2 d2 = entityE.getD();
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
		cfg.addResource( "org/hibernate/test/instrument/domain/ComplexLazyGraph2.hbm.xml" );
	}

	private void prepareTestData() {
		inTransaction(
				getFactory(),
				new Consumer<Session>() {
					@Override
					public void accept(Session session) {
						DEntity2 d = new DEntity2();
						d.setD("bla");
						d.setOid(1);

						byte[] lBytes = "agdfagdfagfgafgsfdgasfdgfgasdfgadsfgasfdgasfdgasdasfdg".getBytes();
						Blob lBlob = Hibernate.getLobCreator(session).createBlob(lBytes);
						d.setBlob(lBlob);

						BEntity2 b1 = new BEntity2();
						b1.setOid(1);
						b1.setB1(34);
						b1.setB2("huhu");

						BEntity2 b2 = new BEntity2();
						b2.setOid(2);
						b2.setB1(37);
						b2.setB2("haha");

						Set<BEntity2> lBs = new HashSet<BEntity2>();
						lBs.add(b1);
						lBs.add(b2);
						d.setBs(lBs);

						AEntity2 a = new AEntity2();
						a.setOid(1);
						a.setA("hihi");
						d.setA(a);

						EEntity2 e = new EEntity2();
						e.setOid(17);
						e.setE1("Balu");
						e.setE2("Bär");

						e.setD(d);
						d.setE(e);

						CEntity2 c = new CEntity2();
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
