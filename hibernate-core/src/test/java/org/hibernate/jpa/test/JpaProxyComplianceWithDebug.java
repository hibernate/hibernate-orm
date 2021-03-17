/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.TestForIssue;
import org.junit.Before;
import org.junit.Test;

@TestForIssue(jiraKey = "HHH-13244")
public class JpaProxyComplianceWithDebug extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected void addConfigOptions(Map options) {
		options.put(
				AvailableSettings.JPA_PROXY_COMPLIANCE,
				Boolean.TRUE);
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				MvnoBillingAgreement.class,
				MvnoOpcio.class,
		};
	}

	@Before
	public void setUp() {
		List<Integer> opciok = Arrays.asList(2008, 2010, 2012, 2014, 2015, 2026, 2027, 2103, 2110, 2145, 992068, 992070);

		doInJPA(this::entityManagerFactory, entityManager -> {

			MvnoBillingAgreement ba = new MvnoBillingAgreement();
			ba.setId(1);
			ba.setName("1");
			entityManager.persist(ba);

			for (int opcioId : opciok) {
				MvnoOpcio o = new MvnoOpcio();
				o.setId(opcioId);
				o.setMegnevezes(Integer.toString(opcioId));
				o.getMvnoBillingAgreementekDefaultOpcioja().add(ba);
				ba.getMvnoDefaultUniverzalisOpcioi().add(o);
				entityManager.persist(o);
			}

			ba.setBehajtasEgyiranyusitasOpcio(entityManager.find(MvnoOpcio.class, 2026));
			ba.setBehajtasFelfuggesztesOpcio(entityManager.find(MvnoOpcio.class, 992070));
			ba.setHotlimitEmeltDijasBarOpcio(entityManager.find(MvnoOpcio.class, 2145));

		});
	}


	@Test
	@TestForIssue(jiraKey = "HHH-13244")
	public void testJpaComplianceProxyWithDebug() {

		//This could be replaced with setting the root logger level, or the "org.hibernate" logger to debug.
		//These are simply the narrowest log settings that trigger the bug
		Logger entityLogger = LogManager.getLogger("org.hibernate.internal.util.EntityPrinter");
		Logger listenerLogger = LogManager.getLogger("org.hibernate.event.internal.AbstractFlushingEventListener");

		Level oldEntityLogLevel = entityLogger.getLevel();
		Level oldListenerLogLevel = listenerLogger.getLevel();

		entityLogger.setLevel((Level) Level.DEBUG);
		listenerLogger.setLevel((Level) Level.DEBUG);
		try {
			doInJPA(this::entityManagerFactory, entityManager -> {
				entityManager.find(MvnoBillingAgreement.class, 1);
			});
		} finally {
			entityLogger.setLevel(oldEntityLogLevel);
			listenerLogger.setLevel(oldListenerLogLevel);
		}

	}

	@Entity
	@Table(name = "mvno_billing_agreement")
	public static class MvnoBillingAgreement implements Serializable {
		private static final long serialVersionUID = 1L;

		@Id
		private int id;

		private String name;

		@ManyToMany
		@JoinTable(
				name = "mvnobillagr_def_univerzalis", joinColumns = {
						@JoinColumn(name = "billing_agreement_id")
				},
				inverseJoinColumns = {
						@JoinColumn(name = "univerzalis_opcio_id")
				})
		private Set<MvnoOpcio> mvnoDefaultUniverzalisOpcioi = new HashSet<>();

		@JoinColumn(name = "egyiranyusitas_opcio_id")
		@ManyToOne(fetch = FetchType.LAZY)
		private MvnoOpcio behajtasEgyiranyusitasOpcio;

		@JoinColumn(name = "felfuggesztes_opcio_id")
		@ManyToOne(fetch = FetchType.LAZY)
		private MvnoOpcio behajtasFelfuggesztesOpcio;

		@JoinColumn(name = "emeltdijas_bar_opcio_id")
		@ManyToOne(fetch = FetchType.LAZY)
		private MvnoOpcio hotlimitEmeltDijasBarOpcio;

		public MvnoBillingAgreement() {}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Set<MvnoOpcio> getMvnoDefaultUniverzalisOpcioi() {
			return this.mvnoDefaultUniverzalisOpcioi;
		}

		public void setMvnoDefaultUniverzalisOpcioi(Set<MvnoOpcio> mvnoDefaultUniverzalisOpcioi) {
			this.mvnoDefaultUniverzalisOpcioi = mvnoDefaultUniverzalisOpcioi;
		}

		public MvnoOpcio getBehajtasEgyiranyusitasOpcio() {
			return this.behajtasEgyiranyusitasOpcio;
		}

		public void setBehajtasEgyiranyusitasOpcio(MvnoOpcio behajtasEgyiranyusitasOpcio) {
			this.behajtasEgyiranyusitasOpcio = behajtasEgyiranyusitasOpcio;
		}

		public MvnoOpcio getBehajtasFelfuggesztesOpcio() {
			return this.behajtasFelfuggesztesOpcio;
		}

		public void setBehajtasFelfuggesztesOpcio(MvnoOpcio behajtasFelfuggesztesOpcio) {
			this.behajtasFelfuggesztesOpcio = behajtasFelfuggesztesOpcio;
		}

		public MvnoOpcio getHotlimitEmeltDijasBarOpcio() {
			return this.hotlimitEmeltDijasBarOpcio;
		}

		public void setHotlimitEmeltDijasBarOpcio(MvnoOpcio hotlimitEmeltDijasBarOpcio) {
			this.hotlimitEmeltDijasBarOpcio = hotlimitEmeltDijasBarOpcio;
		}

	}

	@Entity
	@Table(name = "mvno_opcio")
	public static class MvnoOpcio implements Serializable {
		private static final long serialVersionUID = 1L;

		@Id
		private int id;

		@Column(name = "megnevezes")
		private String megnevezes;

		@ManyToMany(mappedBy = "mvnoDefaultUniverzalisOpcioi")
		private Set<MvnoBillingAgreement> mvnoBillingAgreementekDefaultOpcioja = new HashSet<>();

		public MvnoOpcio() {}

		public int getId() {
			return this.id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public String getMegnevezes() {
			return this.megnevezes;
		}

		public void setMegnevezes(String megnevezes) {
			this.megnevezes = megnevezes;
		}

		public Set<MvnoBillingAgreement> getMvnoBillingAgreementekDefaultOpcioja() {
			return this.mvnoBillingAgreementekDefaultOpcioja;
		}

		public void setMvnoBillingAgreementekDefaultOpcioja(Set<MvnoBillingAgreement> mvnoBillingAgreementekDefaultOpcioja) {
			this.mvnoBillingAgreementekDefaultOpcioja = mvnoBillingAgreementekDefaultOpcioja;
		}

	}


}
