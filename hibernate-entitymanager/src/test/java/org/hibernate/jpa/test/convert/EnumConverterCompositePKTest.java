/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.test.convert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.AttributeConverter;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.test.PersistenceUnitDescriptorAdapter;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

/**
 * Test simple application of Convert annotation via XML.
 *
 * @author Frank Langelage
 */
@TestForIssue(jiraKey = "HHH-8820")
public class EnumConverterCompositePKTest extends BaseUnitTestCase {

	@Test
	public void testEnumConverterInCompositePK() {
		final PersistenceUnitDescriptorAdapter pu = new PersistenceUnitDescriptorAdapter() {

			@Override
			public List<String> getManagedClassNames() {
				return Arrays.asList( MbiAfltg.class.getName(), MbiTermiEnum.class.getName(), MbiTermiInt.class.getName() );
			}
		};

		final Map settings = new HashMap();
		settings.put( AvailableSettings.HBM2DDL_AUTO, "create-drop" );

		EntityManagerFactory emf = Bootstrap.getEntityManagerFactoryBuilder( pu, settings ).build();
		try {
			// create data first
			EntityManager em = emf.createEntityManager();
			em.getTransaction().begin();

			MbiAfltg mbiAfltg = new MbiAfltg();
			// set key values
			mbiAfltg.setAfstaSerial( 4711 );
			mbiAfltg.setLtGrpNr( 1 );
			// set data fields
			mbiAfltg.setTourNr( 42 );

			MbiTermiInt mbiTermi1 = new MbiTermiInt();
			// set key values
			mbiTermi1.setAfstaSerial( mbiAfltg.getAfstaSerial() );
			mbiTermi1.setLtGrpNr( mbiAfltg.getLtGrpNr() );
			mbiTermi1.setTerminArt( 34 );
			// set data fields
			mbiTermi1.setVerfahren( 42 );

			MbiTermiInt mbiTermi2 = new MbiTermiInt();
			// set key values
			mbiTermi2.setAfstaSerial( mbiAfltg.getAfstaSerial() );
			mbiTermi2.setLtGrpNr( mbiAfltg.getLtGrpNr() );
			mbiTermi2.setTerminArt( 40 );
			// set data fields
			mbiTermi2.setVerfahren( 42 );

			em.persist( mbiAfltg );
			em.persist( mbiTermi1 );
			em.persist( mbiTermi2 );

			em.getTransaction().commit();
			em.close();

			em = emf.createEntityManager();
			em.getTransaction().begin();

			// retrieve data now
			// direct access should work
			MbiTermiEnum mbiTermiEnum = em.find( MbiTermiEnum.class, new MbiTermiEnum.PK( 4711, 1, DeliveryDateType.TERMIN_ART_BASISTERMIN ) );

			// access through OneToMany does not work
			mbiAfltg = em.find( MbiAfltg.class, new MbiAfltg.PK( 4711, 1 ) );
			for ( MbiTermiEnum mbiTermi : mbiAfltg.getMbiTermi() ) {
				assertTrue( mbiTermi.getTerminArt().equals( DeliveryDateType.TERMIN_ART_BASISTERMIN )
						|| mbiTermi.getTerminArt().equals( DeliveryDateType.TERMIN_ART_STEUER ) );
			}

			em.getTransaction().commit();
			em.close();
		}
		finally {
			emf.close();
		}
	}

	public enum DeliveryDateType {
		TERMIN_ART_BASISTERMIN(34), TERMIN_ART_STEUER(40);

		final Integer value;

		DeliveryDateType(final int i) {
			this.value = Integer.valueOf( i );
		}

		@javax.persistence.Converter(autoApply = true)
		public static class Converter implements AttributeConverter<DeliveryDateType, Integer> {

			@Override
			public Integer convertToDatabaseColumn(final DeliveryDateType attribute) {
				return attribute.value;
			}

			@Override
			public DeliveryDateType convertToEntityAttribute(final Integer dbValue) {
				DeliveryDateType result = null;
				if ( dbValue != null ) {
					for ( DeliveryDateType enumInstance : DeliveryDateType.values() ) {
						if ( dbValue.equals( enumInstance.value ) ) {
							result = enumInstance;
							break;
						}
					}
				}
				return result;
			}
		}
	}

	@Entity
	@IdClass(value = MbiAfltg.PK.class)
	@Table(name = "mbi_afltg")
	public static class MbiAfltg implements Serializable {

		private static final long serialVersionUID = 1L;

		@Id
		@Column(name = "afsta_serial", nullable = false)
		private Integer afstaSerial;

		@Id
		@Column(name = "lt_grp_nr", nullable = false)
		private Integer ltGrpNr;

		@Column(name = "tour_nr", nullable = false)
		private Integer tourNr;

		@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
		@OrderBy("afstaSerial ASC, ltGrpNr ASC, terminArt ASC")
		@JoinColumns({ @JoinColumn(name = "afsta_serial", referencedColumnName = "afsta_serial", insertable = false, updatable = false),
				@JoinColumn(name = "lt_grp_nr", referencedColumnName = "lt_grp_nr", insertable = false, updatable = false) })
		private Collection<MbiTermiEnum> mbiTermi;

		public Integer getAfstaSerial() {
			return this.afstaSerial;
		}

		public void setAfstaSerial(final Integer afstaSerial) {
			this.afstaSerial = afstaSerial;
		}

		public Integer getLtGrpNr() {
			return this.ltGrpNr;
		}

		public void setLtGrpNr(final Integer ltGrpNr) {
			this.ltGrpNr = ltGrpNr;
		}

		public Integer getTourNr() {
			return this.tourNr;
		}

		public void setTourNr(final Integer tourNr) {
			this.tourNr = tourNr;
		}

		public Collection<MbiTermiEnum> getMbiTermi() {
			return this.mbiTermi;
		}

		public void setMbiTermi(final Collection<MbiTermiEnum> mbiTermi) {
			this.mbiTermi = mbiTermi;
		}

		public static class PK implements Serializable {

			private static final long serialVersionUID = 1L;

			private Integer afstaSerial;

			private Integer ltGrpNr;

			public PK() {
			}

			public PK(final Integer afstaSerial, final Integer ltGrpNr) {
				this.afstaSerial = afstaSerial;
				this.ltGrpNr = ltGrpNr;
			}

			public Integer getAfstaSerial() {
				return this.afstaSerial;
			}

			public void setAfstaSerial(final Integer afstaSerial) {
				this.afstaSerial = afstaSerial;
			}

			public Integer getLtGrpNr() {
				return this.ltGrpNr;
			}

			public void setLtGrpNr(final Integer ltGrpNr) {
				this.ltGrpNr = ltGrpNr;
			}

			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result + ( ( afstaSerial == null ) ? 0 : afstaSerial.hashCode() );
				result = prime * result + ( ( ltGrpNr == null ) ? 0 : ltGrpNr.hashCode() );
				return result;
			}

			@Override
			public boolean equals(Object obj) {
				if ( this == obj )
					return true;
				if ( obj == null )
					return false;
				if ( getClass() != obj.getClass() )
					return false;
				PK other = (PK) obj;
				if ( afstaSerial == null ) {
					if ( other.afstaSerial != null )
						return false;
				}
				else if ( !afstaSerial.equals( other.afstaSerial ) )
					return false;
				if ( ltGrpNr == null ) {
					if ( other.ltGrpNr != null )
						return false;
				}
				else if ( !ltGrpNr.equals( other.ltGrpNr ) )
					return false;
				return true;
			}
		}
	}

	@Entity
	@IdClass(value = MbiTermiEnum.PK.class)
	@Table(name = "mbi_termi")
	public static class MbiTermiEnum implements Serializable {

		private static final long serialVersionUID = 1L;

		@Id
		@Column(name = "afsta_serial", nullable = false)
		private Integer afstaSerial;

		@Id
		@Column(name = "lt_grp_nr", nullable = false)
		private Integer ltGrpNr;

		@Id
		@Column(name = "termin_art", nullable = false)
		private DeliveryDateType terminArt;

		@Column(name = "verfahren", nullable = false)
		private Integer verfahren;

		public Integer getAfstaSerial() {
			return this.afstaSerial;
		}

		public void setAfstaSerial(final Integer afstaSerial) {
			this.afstaSerial = afstaSerial;
		}

		public Integer getLtGrpNr() {
			return this.ltGrpNr;
		}

		public void setLtGrpNr(final Integer ltGrpNr) {
			this.ltGrpNr = ltGrpNr;
		}

		public DeliveryDateType getTerminArt() {
			return this.terminArt;
		}

		public void setTerminArt(final DeliveryDateType terminArt) {
			this.terminArt = terminArt;
		}

		public Integer getVerfahren() {
			return this.verfahren;
		}

		public void setVerfahren(final Integer verfahren) {
			this.verfahren = verfahren;
		}

		public static class PK implements Serializable {

			private static final long serialVersionUID = 1L;

			private Integer afstaSerial;

			private Integer ltGrpNr;

			private DeliveryDateType terminArt;

			public PK() {
			}

			public PK(final Integer afstaSerial, final Integer ltGrpNr, final DeliveryDateType terminArt) {
				this.afstaSerial = afstaSerial;
				this.ltGrpNr = ltGrpNr;
				this.terminArt = terminArt;
			}

			public Integer getAfstaSerial() {
				return this.afstaSerial;
			}

			public void setAfstaSerial(final Integer afstaSerial) {
				this.afstaSerial = afstaSerial;
			}

			public Integer getLtGrpNr() {
				return this.ltGrpNr;
			}

			public void setLtGrpNr(final Integer ltGrpNr) {
				this.ltGrpNr = ltGrpNr;
			}

			public DeliveryDateType getTerminArt() {
				return this.terminArt;
			}

			public void setTerminArt(final DeliveryDateType terminArt) {
				this.terminArt = terminArt;
			}

			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result + ( ( afstaSerial == null ) ? 0 : afstaSerial.hashCode() );
				result = prime * result + ( ( ltGrpNr == null ) ? 0 : ltGrpNr.hashCode() );
				result = prime * result + ( ( terminArt == null ) ? 0 : terminArt.hashCode() );
				return result;
			}

			@Override
			public boolean equals(Object obj) {
				if ( this == obj )
					return true;
				if ( obj == null )
					return false;
				if ( getClass() != obj.getClass() )
					return false;
				PK other = (PK) obj;
				if ( afstaSerial == null ) {
					if ( other.afstaSerial != null )
						return false;
				}
				else if ( !afstaSerial.equals( other.afstaSerial ) )
					return false;
				if ( ltGrpNr == null ) {
					if ( other.ltGrpNr != null )
						return false;
				}
				else if ( !ltGrpNr.equals( other.ltGrpNr ) )
					return false;
				if ( terminArt != other.terminArt )
					return false;
				return true;
			}
		}
	}

	@Entity
	@IdClass(value = MbiTermiInt.PK.class)
	@Table(name = "mbi_termi")
	public static class MbiTermiInt implements Serializable {

		private static final long serialVersionUID = 1L;

		@Id
		@Column(name = "afsta_serial", nullable = false)
		private Integer afstaSerial;

		@Id
		@Column(name = "lt_grp_nr", nullable = false)
		private Integer ltGrpNr;

		@Id
		@Column(name = "termin_art", nullable = false)
		private Integer terminArt;

		@Column(name = "verfahren", nullable = false)
		private Integer verfahren;

		public Integer getAfstaSerial() {
			return this.afstaSerial;
		}

		public void setAfstaSerial(final Integer afstaSerial) {
			this.afstaSerial = afstaSerial;
		}

		public Integer getLtGrpNr() {
			return this.ltGrpNr;
		}

		public void setLtGrpNr(final Integer ltGrpNr) {
			this.ltGrpNr = ltGrpNr;
		}

		public Integer getTerminArt() {
			return this.terminArt;
		}

		public void setTerminArt(final Integer terminArt) {
			this.terminArt = terminArt;
		}

		public Integer getVerfahren() {
			return this.verfahren;
		}

		public void setVerfahren(final Integer verfahren) {
			this.verfahren = verfahren;
		}

		public static class PK implements Serializable {

			private static final long serialVersionUID = 1L;

			private Integer afstaSerial;

			private Integer ltGrpNr;

			private Integer terminArt;

			public PK() {
			}

			public PK(final Integer afstaSerial, final Integer ltGrpNr, final Integer terminArt) {
				this.afstaSerial = afstaSerial;
				this.ltGrpNr = ltGrpNr;
				this.terminArt = terminArt;
			}

			public Integer getAfstaSerial() {
				return this.afstaSerial;
			}

			public void setAfstaSerial(final Integer afstaSerial) {
				this.afstaSerial = afstaSerial;
			}

			public Integer getLtGrpNr() {
				return this.ltGrpNr;
			}

			public void setLtGrpNr(final Integer ltGrpNr) {
				this.ltGrpNr = ltGrpNr;
			}

			public Integer getTerminArt() {
				return this.terminArt;
			}

			public void setTerminArt(final Integer terminArt) {
				this.terminArt = terminArt;
			}

			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result + ( ( afstaSerial == null ) ? 0 : afstaSerial.hashCode() );
				result = prime * result + ( ( ltGrpNr == null ) ? 0 : ltGrpNr.hashCode() );
				result = prime * result + ( ( terminArt == null ) ? 0 : terminArt.hashCode() );
				return result;
			}

			@Override
			public boolean equals(Object obj) {
				if ( this == obj )
					return true;
				if ( obj == null )
					return false;
				if ( getClass() != obj.getClass() )
					return false;
				PK other = (PK) obj;
				if ( afstaSerial == null ) {
					if ( other.afstaSerial != null )
						return false;
				}
				else if ( !afstaSerial.equals( other.afstaSerial ) )
					return false;
				if ( ltGrpNr == null ) {
					if ( other.ltGrpNr != null )
						return false;
				}
				else if ( !ltGrpNr.equals( other.ltGrpNr ) )
					return false;
				if ( terminArt != other.terminArt )
					return false;
				return true;
			}
		}
	}
}
