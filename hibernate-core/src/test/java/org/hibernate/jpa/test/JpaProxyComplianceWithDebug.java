package org.hibernate.jpa.test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;
import java.math.BigDecimal;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.SharedCacheMode;
import javax.persistence.Table;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceUnitTransactionType;

@TestForIssue(jiraKey = "HHH-13244")
public class JpaProxyComplianceWithDebug extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected void addConfigOptions(Map options) {
		options.put(
				AvailableSettings.JPA_PROXY_COMPLIANCE,
				Boolean.TRUE);
	}

	public static class TestingPersistenceUnitDescriptorImplTest implements PersistenceUnitDescriptor {
		private final String name;

		public TestingPersistenceUnitDescriptorImplTest(String name) {
			this.name = name;
		}

		@Override
		public SharedCacheMode getSharedCacheMode() {
			return SharedCacheMode.NONE;
		}

		@Override
		public URL getPersistenceUnitRootUrl() {
			return null;
		}

		@Override
		public String getName() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getProviderClassName() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean isUseQuotedIdentifiers() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean isExcludeUnlistedClasses() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public PersistenceUnitTransactionType getTransactionType() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ValidationMode getValidationMode() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<String> getManagedClassNames() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<String> getMappingFileNames() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<URL> getJarFileUrls() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Object getNonJtaDataSource() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Object getJtaDataSource() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Properties getProperties() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ClassLoader getClassLoader() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ClassLoader getTempClassLoader() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void pushClassTransformer(EnhancementContext enhancementContext) {
			// TODO Auto-generated method stub

		}

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

		//This could be replaced with setting the root logger level, or the org.hibernate logger debug.
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
		} catch (Exception e) {
			Assert.fail("got exception " + e.toString());
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

		private String leiras;

		@ManyToMany(mappedBy = "mvnoBillingAgreements")
		private Set<MvnoOpcio> mvnoOpcioi = new HashSet<>();

		@ManyToMany
		@JoinTable(
				name = "mvno_billing_agreement_default_univerzalis_opcio", joinColumns = {
						@JoinColumn(name = "billing_agreement_id")
				},
				inverseJoinColumns = {
						@JoinColumn(name = "univerzalis_opcio_id")
				})
		private Set<MvnoOpcio> mvnoDefaultUniverzalisOpcioi = new HashSet<>();

		/* Behajtas barring opciok */

		@JoinColumn(name = "behajtas_egyiranyusitas_opcio_id")
		@ManyToOne(fetch = FetchType.LAZY)
		private MvnoOpcio behajtasEgyiranyusitasOpcio;

		@JoinColumn(name = "behajtas_felfuggesztes_opcio_id")
		@ManyToOne(fetch = FetchType.LAZY)
		private MvnoOpcio behajtasFelfuggesztesOpcio;

		/* Hotlimit barring opciok */

		@JoinColumn(name = "hotlimit_normal_bar_opcio_id")
		@ManyToOne(fetch = FetchType.LAZY)
		private MvnoOpcio hotlimitNormalBarOpcio;

		@JoinColumn(name = "hotlimit_emeltdijas_bar_opcio_id")
		@ManyToOne(fetch = FetchType.LAZY)
		private MvnoOpcio hotlimitEmeltDijasBarOpcio;

		/* Radius roaming data barring opcio */

		@JoinColumn(name = "radius_roaming_data_bar_opcio_id")
		@ManyToOne(fetch = FetchType.LAZY)
		private MvnoOpcio radiusRoamingDataBarOpcio;

		/* Szerzodes szuneteltetes opcio */

		@JoinColumn(name = "szerzodes_szuneteltetes_opcio_id")
		@ManyToOne(fetch = FetchType.LAZY)
		private MvnoOpcio szerzodesSzuneteltetesOpcio;

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

		public String getLeiras() {
			return leiras;
		}

		public void setLeiras(String leiras) {
			this.leiras = leiras;
		}

		public Set<MvnoOpcio> getMvnoOpcioi() {
			return mvnoOpcioi;
		}

		public void setMvnoOpcioi(Set<MvnoOpcio> mvnoOpcioi) {
			this.mvnoOpcioi = mvnoOpcioi;
		}

		public void addMvnoOpcio(MvnoOpcio mvnoOpcio) {
			this.getMvnoOpcioi().add(mvnoOpcio);
		}

		public void removeMvnoOpcio(MvnoOpcio mvnoOpcio) {
			this.getMvnoOpcioi().remove(mvnoOpcio);
		}

		public Set<MvnoOpcio> getMvnoDefaultUniverzalisOpcioi() {
			return this.mvnoDefaultUniverzalisOpcioi;
		}

		public void setMvnoDefaultUniverzalisOpcioi(Set<MvnoOpcio> mvnoDefaultUniverzalisOpcioi) {
			this.mvnoDefaultUniverzalisOpcioi = mvnoDefaultUniverzalisOpcioi;
		}

		public void addMvnoDefaultUniverzalisOpcioi(MvnoOpcio mvnoDefaultUniverzalisOpcioi) {
			this.getMvnoDefaultUniverzalisOpcioi().add(mvnoDefaultUniverzalisOpcioi);
			mvnoDefaultUniverzalisOpcioi.addMvnoBillingAgreementekDefaultOpcioja(this);
		}

		public void removeMvnoDefaultUniverzalisOpcioi(MvnoOpcio mvnoDefaultUniverzalisOpcioi) {
			this.getMvnoDefaultUniverzalisOpcioi().remove(mvnoDefaultUniverzalisOpcioi);
			mvnoDefaultUniverzalisOpcioi.removeMvnoBillingAgreementekDefaultOpcioja(this);
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

		public MvnoOpcio getHotlimitNormalBarOpcio() {
			return this.hotlimitNormalBarOpcio;
		}

		public void setHotlimitNormalBarOpcio(MvnoOpcio hotlimitNormalBarOpcio) {
			this.hotlimitNormalBarOpcio = hotlimitNormalBarOpcio;
		}

		public MvnoOpcio getHotlimitEmeltDijasBarOpcio() {
			return this.hotlimitEmeltDijasBarOpcio;
		}

		public void setHotlimitEmeltDijasBarOpcio(MvnoOpcio hotlimitEmeltDijasBarOpcio) {
			this.hotlimitEmeltDijasBarOpcio = hotlimitEmeltDijasBarOpcio;
		}

		public MvnoOpcio getRadiusRoamingDataBarOpcio() {
			return this.radiusRoamingDataBarOpcio;
		}

		public void setRadiusRoamingDataBarOpcio(MvnoOpcio radiusRoamingDataBarOpcio) {
			this.radiusRoamingDataBarOpcio = radiusRoamingDataBarOpcio;
		}

		public MvnoOpcio getSzerzodesSzuneteltetesOpcio() {
			return this.szerzodesSzuneteltetesOpcio;
		}

		public void setSzerzodesSzuneteltetesOpcio(MvnoOpcio szerzodesSzuneteltetesOpcio) {
			this.szerzodesSzuneteltetesOpcio = szerzodesSzuneteltetesOpcio;
		}
		//
		//
		// @Override
		// public int hashCode() {
		// final int prime = 31;
		// int result = 1;
		// result = prime * result + getId();
		// return result;
		// }
		//
		// @Override
		// public boolean equals(Object obj) {
		// if (this == obj) {
		// return true;
		// }
		// if (obj == null) {
		// return false;
		// }
		// if (!(obj instanceof MvnoBillingAgreement)) {
		// return false;
		// }
		// MvnoBillingAgreement other = (MvnoBillingAgreement) obj;
		// if (getId() != other.getId()) {
		// return false;
		// }
		// return true;
		// }

	}

	@Entity
	@Table(name = "mvno_opcio")
	public static class MvnoOpcio implements Serializable {
		private static final long serialVersionUID = 1L;

		@Id
		private int id;

		@Column(name = "megnevezes")
		private String megnevezes;


		@Column(name = "szamlazasi_mod")
		private String szamlazasiMod;

		@Column(name = "mukodesi_mod")
		private String mukodesiMod;

		@Column(name = "megjelenitesi_sorrend")
		private int megjelenitesiSorrend;

		@Column(name = "cug_merete")
		private int cugMerete;

		@Column(name = "cug_zona_id")
		private Integer cugZonaId;

		@Column(name = "idohatar")
		private int idohatar;

		@Column(name = "hatarozott_idotartam_ho")
		private int hatarozottIdotartamHo; // hónap

		@Column(name = "breakdown_code_2")
		private String breakdownCode2;

		// @Enumerated(EnumType.ORDINAL)
		@Column(name = "jutalek_tipus")
		private String jutalekTipus;

		@Column(name = "sms_code", nullable = true)
		private String smsCode;

		@Column(name = "forgalomtol")
		private BigDecimal forgalomtol;

		@Column(name = "forgalomtol_egyseg")
		private String forgalomtolEgyseg;

		@Column(name = "forgalomtol_cdrenkent")
		private String forgalomtolCdrenkent;

		@ManyToMany
		@JoinTable(
				name = "mvno_opcio_billing_agreement", joinColumns = {
						@JoinColumn(name = "opcio_id")
				},
				inverseJoinColumns = {
						@JoinColumn(name = "billing_agreement_id")
				})
		private Set<MvnoBillingAgreement> mvnoBillingAgreements;

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

		public String getSzamlazasiMod() {
			return szamlazasiMod;
		}

		public void setSzamlazasiMod(String szamlazasiMod) {
			this.szamlazasiMod = szamlazasiMod;
		}

		public String getMukodesiMod() {
			return mukodesiMod;
		}

		public void setMukodesiMod(String mukodesiMod) {
			this.mukodesiMod = mukodesiMod;
		}

		public int getMegjelenitesiSorrend() {
			return megjelenitesiSorrend;
		}

		public void setMegjelenitesiSorrend(int megjelenitesiSorrend) {
			this.megjelenitesiSorrend = megjelenitesiSorrend;
		}

		public int getCugMerete() {
			return cugMerete;
		}

		public void setCugMerete(int cugMerete) {
			this.cugMerete = cugMerete;
		}

		public Integer getCugZonaId() {
			return cugZonaId;
		}

		public void setCugZonaId(Integer cugZonaId) {
			this.cugZonaId = cugZonaId;
		}

		public int getIdohatar() {
			return idohatar;
		}

		public void setIdohatar(int idohatar) {
			this.idohatar = idohatar;
		}

		public String getBreakdownCode2() {
			return breakdownCode2;
		}

		public void setBreakdownCode2(String breakdownCode2) {
			this.breakdownCode2 = breakdownCode2;
		}

		public int getHatarozottIdotartamHo() {
			return hatarozottIdotartamHo;
		}

		public void setHatarozottIdotartamHo(int hatarozottIdotartamHo) {
			this.hatarozottIdotartamHo = hatarozottIdotartamHo;
		}



		public String getSmsCode() {
			return this.smsCode;
		}

		public void setSmsCode(String smsCode) {
			this.smsCode = smsCode;
		}

		public BigDecimal getForgalomtol() {
			return forgalomtol;
		}

		public void setForgalomtol(BigDecimal forgalomtol) {
			this.forgalomtol = forgalomtol;
		}

		public String getForgalomtolEgyseg() {
			return forgalomtolEgyseg;
		}

		public void setForgalomtolEgyseg(String forgalomtolEgyseg) {
			this.forgalomtolEgyseg = forgalomtolEgyseg;
		}

		public String getForgalomtolCdrenkent() {
			return forgalomtolCdrenkent;
		}

		public boolean isForgalomtolCdrenkent() {
			return "igen".equals(getForgalomtolCdrenkent());
		}

		public void setForgalomtolCdrenkent(String forgalomtolCdrenkent) {
			this.forgalomtolCdrenkent = forgalomtolCdrenkent;
		}

		public Set<MvnoBillingAgreement> getMvnoBillingAgreements() {
			return this.mvnoBillingAgreements;
		}

		public void setMvnoBillingAgreements(Set<MvnoBillingAgreement> mvnoBillingAgreements) {
			this.mvnoBillingAgreements = mvnoBillingAgreements;
		}

		public void addMvnoBillingAgreement(MvnoBillingAgreement mvnoBillingAgreement) {
			this.getMvnoBillingAgreements().add(mvnoBillingAgreement);
			mvnoBillingAgreement.addMvnoOpcio(this);
		}

		public void removeMvnoBillingAgreement(MvnoBillingAgreement mvnoBillingAgreement) {
			this.getMvnoBillingAgreements().remove(mvnoBillingAgreement);
			mvnoBillingAgreement.removeMvnoOpcio(this);
		}

		public Set<MvnoBillingAgreement> getMvnoBillingAgreementekDefaultOpcioja() {
			return this.mvnoBillingAgreementekDefaultOpcioja;
		}

		public void setMvnoBillingAgreementekDefaultOpcioja(Set<MvnoBillingAgreement> mvnoBillingAgreementekDefaultOpcioja) {
			this.mvnoBillingAgreementekDefaultOpcioja = mvnoBillingAgreementekDefaultOpcioja;
		}

		public void addMvnoBillingAgreementekDefaultOpcioja(MvnoBillingAgreement mvnoBillingAgreementekDefaultOpcioja) {
			this.getMvnoBillingAgreementekDefaultOpcioja().add(mvnoBillingAgreementekDefaultOpcioja);
		}

		public void removeMvnoBillingAgreementekDefaultOpcioja(MvnoBillingAgreement mvnoBillingAgreementekDefaultOpcioja) {
			this.getMvnoBillingAgreementekDefaultOpcioja().remove(mvnoBillingAgreementekDefaultOpcioja);
		}

		public String getJutalekTipus() {
			return jutalekTipus;
		}

		public void setJutalekTipus(String jutalekTipus) {
			this.jutalekTipus = jutalekTipus;
		}

		// @Override
		// public int hashCode() {
		// final int prime = 31;
		// int result = 1;
		// result = prime * result + getId();
		// return result;
		// }
		//
		// @Override
		// public boolean equals(Object obj) {
		// if (this == obj) {
		// return true;
		// }
		// if (obj == null) {
		// return false;
		// }
		// if (!(obj instanceof MvnoOpcio)) {
		// return false;
		// }
		// MvnoOpcio other = (MvnoOpcio) obj;
		// if (getId() != other.getId()) {
		// return false;
		// }
		// return true;
		// }

	}


}
