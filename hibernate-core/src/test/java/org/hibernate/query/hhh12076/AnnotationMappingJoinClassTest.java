/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.hhh12076;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertNotNull;

@TestForIssue(jiraKey = "HHH-12076")
public class AnnotationMappingJoinClassTest extends BaseCoreFunctionalTestCase {

    @Override
    protected Class[] getAnnotatedClasses() {
        return new Class[]{
            Claim.class,
            Settlement.class,
            Task.class,
            SettlementTask.class,
            TaskStatus.class,
            Extension.class,
            SettlementExtension.class,
            GapAssessmentExtension.class,
            EwtAssessmentExtension.class
        };
    }

    @Override
    protected void prepareTest() {
        doInHibernate( this::sessionFactory, session -> {
            TaskStatus taskStatus = new TaskStatus();
            taskStatus.setName("Enabled");
            taskStatus.setDisplayName("Enabled");
            session.save(taskStatus);

            for (long i = 0; i < 10; i++) {
                SettlementTask settlementTask = new SettlementTask();
                settlementTask.setId(i);
                Settlement settlement = new Settlement();
                settlementTask.setLinked(settlement);
                settlementTask.setStatus(taskStatus);

                Claim claim = new Claim();
                claim.setId(i);
                settlement.setClaim(claim);

                for (int j = 0; j < 2; j++) {
                    GapAssessmentExtension gapAssessmentExtension = new GapAssessmentExtension();
                    gapAssessmentExtension.setSettlement(settlement);
                    EwtAssessmentExtension ewtAssessmentExtension = new EwtAssessmentExtension();
                    ewtAssessmentExtension.setSettlement(settlement);

                    settlement.getExtensions().add(gapAssessmentExtension);
                    settlement.getExtensions().add(ewtAssessmentExtension);
                }
                session.save(claim);
                session.save(settlement);
                session.save(settlementTask);
            }
        } );
    }

    @Test
    public void testClassExpressionInOnClause() {
        doInHibernate( this::sessionFactory, session -> {
            List<SettlementTask> results = session.createQuery(
                "select " +
                "   rootAlias.id, " +
                "   linked.id, " +
                "   extensions.id " +
                "from SettlementTask as rootAlias " +
                "join rootAlias.linked as linked " +
                "left join linked.extensions as extensions on extensions.class = EwtAssessmentExtension " +
                "where linked.id = :claimId "
            )
            .setParameter("claimId", 1L)
            .getResultList();

            assertNotNull(results);
        } );
    }

    @Test
    public void testClassExpressionInWhereClause() {
        doInHibernate( this::sessionFactory, session -> {
            List<SettlementTask> results = session.createQuery(
                "select " +
                "   rootAlias.id, " +
                "   linked.id, " +
                "   extensions.id " +
                "from SettlementTask as rootAlias " +
                "join rootAlias.linked as linked " +
                "left join linked.extensions as extensions " +
                "where linked.id = :claimId and (extensions is null or extensions.class = EwtAssessmentExtension)"
            )
            .setParameter("claimId", 1L)
            .getResultList();

            assertNotNull(results);
        } );
    }

    @Entity(name = "Claim")
	@Table(name = "claim")
	public static class Claim {
		public static final long serialVersionUID = 1L;

		@Id
		@GeneratedValue
		private Long id;

		@Version
		private Integer version;

		@CreationTimestamp
		private Date creationDate;

		@UpdateTimestamp
		private Date modifiedDate;

		private Long trackingId;
		private Integer term;
		private Double initialReserve = 0.0;

		@Temporal( TemporalType.DATE )
		private Date effectiveDate;

		@Temporal( TemporalType.DATE )
		private Date expiryDate;

		@Temporal( TemporalType.DATE )
		private Date notificationDate;

		@Temporal( TemporalType.DATE )
		private Date pendingDate;

		@Temporal( TemporalType.DATE )
		private Date openDate;

		@Temporal( TemporalType.DATE )
		private Date suspendDate;

		@Temporal( TemporalType.DATE )
		private Date closeDate;

		private String externalId;
		private String importRef;
		private String location;

		@OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
		private Set<Extension> extensions = new HashSet<>();

		@OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
		private Set<Settlement> settlements = new HashSet<>();

		public Claim getClaim() {
			return this;
		}

		public void addExtension(Extension extension) {
			extensions.add( extension);
			extension.setClaim(this);
		}

		public void addSettlement(Settlement settlement) {
			settlements.add( settlement);
			settlement.setClaim(this);
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Date getCreationDate() {
			return creationDate;
		}

		public void setCreationDate(Date creationDate) {
			this.creationDate = creationDate;
		}

		public Date getModifiedDate() {
			return modifiedDate;
		}

		public void setModifiedDate(Date modifiedDate) {
			this.modifiedDate = modifiedDate;
		}

		public Integer getVersion() {
			return version;
		}

		public void setVersion(Integer version) {
			this.version = version;
		}

		public Long getTrackingId() {
			return trackingId;
		}

		public void setTrackingId(Long trackingId) {
			this.trackingId = trackingId;
		}

		public String getExternalId() {
			return externalId;
		}

		public void setExternalId(String externalId) {
			this.externalId = externalId;
		}

		public Date getEffectiveDate() {
			return effectiveDate;
		}

		public void setEffectiveDate(Date effectiveDate) {
			this.effectiveDate = effectiveDate;
		}

		public Date getExpiryDate() {
			return expiryDate;
		}

		public void setExpiryDate(Date expiryDate) {
			this.expiryDate = expiryDate;
		}

		public Set<Extension> getExtensions() {
			return extensions;
		}

		public void setExtensions(Set<Extension> extensions) {
			this.extensions = extensions;
		}

		public Set<Settlement> getSettlements() {
			return settlements;
		}

		public void setSettlements(Set<Settlement> settlements) {
			this.settlements = settlements;
		}

		public Date getNotificationDate() {
			return notificationDate;
		}

		public void setNotificationDate(Date notificationDate) {
			this.notificationDate = notificationDate;
		}

		public Date getOpenDate() {
			return openDate;
		}

		public void setOpenDate(Date openDate) {
			this.openDate = openDate;
		}

		public Date getCloseDate() {
			return closeDate;
		}

		public void setCloseDate(Date closeDate) {
			this.closeDate = closeDate;
		}

		public Double getInitialReserve() {
			return initialReserve;
		}

		public void setInitialReserve(Double initialReserve) {
			this.initialReserve = initialReserve;
		}

		public String getLocation() {
			return location;
		}

		public void setLocation(String location) {
			this.location = location;
		}

		public String getImportRef() {
			return importRef;
		}

		public void setImportRef(String importRef) {
			this.importRef = importRef;
		}

		public Date getPendingDate() {
			return pendingDate;
		}

		public void setPendingDate(Date startDate) {
			pendingDate = startDate;
		}

		public Date getSuspendDate() {
			return suspendDate;
		}

		public void setSuspendDate(Date suspendDate) {
			this.suspendDate = suspendDate;
		}
	}

    @Entity(name = "EwtAssessmentExtension")
	@Table(name = "claimsettlement_ext_i3_ewt")
	public static class EwtAssessmentExtension extends SettlementExtension {
		public static final long serialVersionUID = 1L;

		private Double requestedUnits = -1.0; //2
		private Double requestedUnitAmount = -1.0; //$150
		private Double requestedSubtotal = 0.0; //$300
		private Double requestedTaxAmount = 0.0; //$30
		private Double requestedTotal = 0.0;  //$330

		private Double coveredRatio = 0.0;
		private Double coveredUnits = 0.0;
		private Double coveredUnitAmount = 0.0;
		private Double coveredUnitAmountOverride = 0.0;
		private Double coveredSubtotal = 0.0;
		private Double coveredTaxAmount = 0.0;
		private Double coveredTotal = 0.0;

		private Double underinsuredAmount = 0.0;
		private Double shortfallUnitAmount = 0.0;
		private Double shortfallTotal = 0.0;

		private Double taxRate = 0.0;

		private String details;
		private String damageType;
		private String exclusion;
		private Boolean validInspection;
		private Boolean taxExempt = false;

		public EwtAssessmentExtension() {
		}

		public Double getRequestedUnits() {
			return requestedUnits;
		}

		public void setRequestedUnits(Double requestedUnits) {
			this.requestedUnits = requestedUnits;
		}

		public Double getRequestedUnitAmount() {
			return requestedUnitAmount;
		}

		public void setRequestedUnitAmount(Double requestedBenefitPerUnit) {
			requestedUnitAmount = requestedBenefitPerUnit;
		}

		public Double getRequestedSubtotal() {
			return requestedSubtotal;
		}

		public void setRequestedSubtotal(Double requestedBenefitSubtotal) {
			requestedSubtotal = requestedBenefitSubtotal;
		}

		public Double getRequestedTaxAmount() {
			return requestedTaxAmount;
		}

		public void setRequestedTaxAmount(Double requestedBenefitTax) {
			requestedTaxAmount = requestedBenefitTax;
		}

		public Double getRequestedTotal() {
			return requestedTotal;
		}

		public void setRequestedTotal(Double requestedBenefitTotal) {
			requestedTotal = requestedBenefitTotal;
		}

		public Double getCoveredUnitAmount() {
			return coveredUnitAmount;
		}

		public void setCoveredUnitAmount(Double coveredBenefitPerUnit) {
			coveredUnitAmount = coveredBenefitPerUnit;
		}

		public Double getCoveredSubtotal() {
			return coveredSubtotal;
		}

		public void setCoveredSubtotal(Double coveredBenefitSubtotal) {
			coveredSubtotal = coveredBenefitSubtotal;
		}

		public Double getCoveredTaxAmount() {
			return coveredTaxAmount;
		}

		public void setCoveredTaxAmount(Double coveredTaxAmount) {
			this.coveredTaxAmount = coveredTaxAmount;
		}

		public Double getCoveredTotal() {
			return coveredTotal;
		}

		public void setCoveredTotal(Double coveredBenefitTotal) {
			coveredTotal = coveredBenefitTotal;
		}

		public Double getTaxRate() {
			return taxRate;
		}

		public void setTaxRate(Double taxRate) {
			this.taxRate = taxRate;
		}

		public Double getShortfallUnitAmount() {
			return shortfallUnitAmount;
		}

		public void setShortfallUnitAmount(Double shortfallUnitAmount) {
			this.shortfallUnitAmount = shortfallUnitAmount;
		}

		public Double getShortfallTotal() {
			return shortfallTotal;
		}

		public void setShortfallTotal(Double shortfallTotal) {
			this.shortfallTotal = shortfallTotal;
		}

		public String getDetails() {
			return details;
		}

		public void setDetails(String description) {
			details = description;
		}

		public Double getUnderinsuredAmount() {
			return underinsuredAmount;
		}

		public void setUnderinsuredAmount(Double truncatedAmount) {
			underinsuredAmount = truncatedAmount;
		}

		public Double getCoveredUnits() {
			return coveredUnits;
		}

		public void setCoveredUnits(Double coveredUnits) {
			this.coveredUnits = coveredUnits;
		}

		public String getDamageType() {
			return damageType;
		}

		public void setDamageType(String damageType) {
			this.damageType = damageType;
		}

		public Double getCoveredRatio() {
			return coveredRatio;
		}

		public void setCoveredRatio(Double coveredRatio) {
			this.coveredRatio = coveredRatio;
		}

		public String getExclusion() {
			return exclusion;
		}

		public void setExclusion(String exclusion) {
			this.exclusion = exclusion;
		}

		public Double getCoveredUnitAmountOverride() {
			return coveredUnitAmountOverride;
		}

		public void setCoveredUnitAmountOverride(Double coveredUnitOverride) {
			coveredUnitAmountOverride = coveredUnitOverride;
		}

		public Boolean isValidInspection() {
			return validInspection;
		}

		public void setValidInspection(Boolean validInspection) {
			this.validInspection = validInspection;
		}

		public Boolean isTaxExempt() {
			return taxExempt;
		}

		public void setTaxExempt(Boolean taxExempt) {
			this.taxExempt = taxExempt;
		}

	}

    @Entity(name = "Extension")
	@Table(name = "claimext")
	@Inheritance(strategy = InheritanceType.JOINED)
	public abstract static class Extension {

		@Id
		@GeneratedValue
		private Long id;

		@Version
		private Integer version;

		@CreationTimestamp
		private Date creationDate;

		@UpdateTimestamp
		private Date modifiedDate;

		private String type;

		@ManyToOne(fetch = FetchType.LAZY)
		private Claim claim;

		public Extension() {
			String[] name = this.getClass().getName().split("\\.");
			type = name[name.length-1];
		}

		public Long getId() {
			return id;
		}

		protected void setId(Long id) {
			this.id = id;
		}

		public Date getCreationDate() {
			return creationDate;
		}

		public void setCreationDate(Date creationDate) {
			this.creationDate = creationDate;
		}

		public Date getModifiedDate() {
			return modifiedDate;
		}

		public void setModifiedDate(Date modifiedDate) {
			this.modifiedDate = modifiedDate;
		}

		public Integer getVersion() {
			return version;
		}

		public void setVersion(Integer version) {
			this.version = version;
		}

		public Claim getClaim() {
			return claim;
		}

		public void setClaim(Claim claim) {
			this.claim = claim;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

	}

    @Entity(name = "GapAssessmentExtension")
	@Table(name = "claim_settlement_ext_gap")
	public static class GapAssessmentExtension extends SettlementExtension {

		private Double insuredsObligation = 0.0;
		private Double eligibleAmount = 0.0;
		private Double assessedAmount = 0.0;
		private Double underinsuredAmount = 0.0;

		public Double getAssessedAmount() {
			return assessedAmount;
		}

		public void setAssessedAmount(Double assessedAmount) {
			this.assessedAmount = assessedAmount;
		}

		public Double getEligibleAmount() {
			return eligibleAmount;
		}

		public void setEligibleAmount(Double eligible) {
			eligibleAmount = eligible;
		}

		public Double getUnderinsuredAmount() {
			return underinsuredAmount;
		}

		public void setUnderinsuredAmount(Double underinsuredAmount) {
			this.underinsuredAmount = underinsuredAmount;
		}

		public Double getInsuredsObligation() {
			return insuredsObligation;
		}

		public void setInsuredsObligation(Double insuredsObligation) {
			this.insuredsObligation = insuredsObligation;
		}
	}

    @Entity(name = "Settlement")
	@Table(name = "claim_settlement")
	public static class Settlement {

		@Id
		@GeneratedValue
		private Long id;

		@Version
		private Integer version;

		@CreationTimestamp
		private Date creationDate;

		@UpdateTimestamp
		private Date modifiedDate;

		private Boolean override = false;
		private Boolean started = false;
		private Boolean taxable = false;

		private Double units = 0.0;
		private Double amount = 0.0;
		private Double subtotal = 0.0;

		private Double taxRate = 0.0;
		private Double taxAmount = 0.0;

		private Double goodwill = 0.0;
		private Double totalAmount = 0.0;
		private Double underinsuredAmount = 0.0;

		@Temporal( TemporalType.TIMESTAMP )
		private Date openDate;

		@Temporal( TemporalType.TIMESTAMP )
		private Date allocateDate;

		@Temporal( TemporalType.TIMESTAMP )
		private Date closeDate;

		private String trackingId;

		@ManyToOne(fetch = FetchType.LAZY)
		private Claim claim;

		@Enumerated(EnumType.STRING)
		private SettlementStatus status = SettlementStatus.RESERVED;

		@OneToMany(mappedBy = "settlement", cascade = CascadeType.ALL, orphanRemoval = true)
		@OrderColumn(name = "orderindex")
		private Set<SettlementExtension> extensions = new HashSet<>();

		private transient Map<Class<?>, SettlementExtension> extensionMap;

		public Long getId() {
			return id;
		}

		protected void setId(Long id) {
			this.id = id;
		}

		public Integer getVersion() {
			return version;
		}

		public void setVersion(Integer version) {
			this.version = version;
		}

		public Date getCreationDate() {
			return creationDate;
		}

		public void setCreationDate(Date creationDate) {
			this.creationDate = creationDate;
		}

		public Date getModifiedDate() {
			return modifiedDate;
		}

		public void setModifiedDate(Date modifiedDate) {
			this.modifiedDate = modifiedDate;
		}

		public Claim getClaim() {
			return claim;
		}

		public void setClaim(Claim claim) {
			this.claim = claim;
		}

		public SettlementStatus getStatus() {
			return status;
		}

		public void setStatus(SettlementStatus status) {
			this.status = status;
		}

		public String getTrackingId() {
			return trackingId;
		}

		public void setTrackingId(String trackingId) {
			this.trackingId = trackingId;
		}

		public Double getUnits() {
			return units;
		}

		public void setUnits(Double units) {
			this.units = units;
		}

		public Double getAmount() {
			return amount;
		}

		public void setAmount(Double amount) {
			this.amount = amount;
		}

		public Double getTotalAmount() {
			return totalAmount;
		}

		public void setTotalAmount(Double totalAmount) {
			this.totalAmount = totalAmount;
		}

		public Date getCloseDate() {
			return closeDate;
		}

		public void setCloseDate(Date settlementDate) {
			closeDate = settlementDate;
		}

		public Set<SettlementExtension> getExtensions() {
			return extensions;
		}

		public void setExtensions(Set<SettlementExtension> extensions) {
			this.extensions = extensions;
		}

		public void addExtension(SettlementExtension extension) {
			if (!hasExtension(extension.getClass())) {
				if (extension.getOrderIndex() == null) {
					extension.setOrderIndex( extensions.size());
				}
				extension.setSettlement(this);
				extensions.add( extension);
			}
		}

		@SuppressWarnings("unchecked")
		public <X extends SettlementExtension> X getExtension(Class<X> extensionType) {
			if (extensionMap == null || extensionMap.size() != extensions.size()) {
				Map<Class<?>, SettlementExtension> map = new HashMap<Class<?>, SettlementExtension>( extensions.size());
				for (SettlementExtension extension : extensions ) {
					map.put(extension.getClass(), extension);
				}
				extensionMap = map;
			}
			return (X)extensionMap.get(extensionType);
		}

		public <X extends SettlementExtension> boolean hasExtension(Class<X> extensionType) {
			return getExtension(extensionType) != null;
		}

		public Boolean isOverride() {
			return override;
		}

		public void setOverride(Boolean override) {
			this.override = override;
		}

		public Double getGoodwill() {
			return goodwill;
		}

		public void setGoodwill(Double goodwill) {
			this.goodwill = goodwill;
		}

		public Date getOpenDate() {
			return openDate;
		}

		public void setOpenDate(Date startDate) {
			openDate = startDate;
		}

		public Date getAllocateDate() {
			return allocateDate;
		}

		public void setAllocateDate(Date allocateDate) {
			this.allocateDate = allocateDate;
		}

		public Double getSubtotal() {
			return subtotal;
		}

		public void setSubtotal(Double subtotal) {
			this.subtotal = subtotal;
		}

		public Double getTaxRate() {
			return taxRate;
		}

		public void setTaxRate(Double taxRate) {
			this.taxRate = taxRate;
		}

		public Double getTaxAmount() {
			return taxAmount;
		}

		public void setTaxAmount(Double taxAmount) {
			this.taxAmount = taxAmount;
		}

		public Double getUnderinsuredAmount() {
			return underinsuredAmount;
		}

		public void setUnderinsuredAmount(Double underinsuredAmount) {
			this.underinsuredAmount = underinsuredAmount;
		}

		public Boolean isStarted() {
			return started;
		}

		public void setStarted(Boolean started) {
			this.started = started;
		}

		public Boolean isTaxable() {
			return taxable;
		}

		public void setTaxable(Boolean taxable) {
			this.taxable = taxable;
		}

	}

    @Entity(name = "SettlementExtension")
	@Table(name = "claimsettlement_ext")
	public abstract static class SettlementExtension {

		@Id
		@GeneratedValue
		private Long id;

		@Version
		private Integer version;

		@CreationTimestamp
		private Date creationDate;

		@UpdateTimestamp
		private Date modifiedDate;

		@Column(name = "order_index")
		private Integer orderIndex;

		@ManyToOne(fetch = FetchType.LAZY)
		private Settlement settlement;

		public SettlementExtension() {
		}

		public Claim getClaim() {
			return settlement.getClaim();
		}

		public Long getId() {
			return id;
		}

		protected void setId(Long id) {
			this.id = id;
		}

		public Integer getVersion() {
			return version;
		}

		public void setVersion(Integer version) {
			this.version = version;
		}

		public Date getCreationDate() {
			return creationDate;
		}

		public void setCreationDate(Date creationDate) {
			this.creationDate = creationDate;
		}

		public Date getModifiedDate() {
			return modifiedDate;
		}

		public void setModifiedDate(Date modifiedDate) {
			this.modifiedDate = modifiedDate;
		}

		public Settlement getSettlement() {
			return settlement;
		}

		public void setSettlement(Settlement settlement) {
			this.settlement = settlement;
		}

		public Integer getOrderIndex() {
			return orderIndex;
		}

		public void setOrderIndex(Integer orderIndex) {
			this.orderIndex = orderIndex;
		}

	}

    public enum SettlementStatus {
		RESERVED, ALLOCATED, PAID, VOID, DENIED
	}

    @Entity(name = "SettlementTask")
	public static class SettlementTask extends Task<Settlement> {

		@ManyToOne(fetch = FetchType.LAZY)
		private Settlement linked;

		public Settlement getLinked() {
			return linked;
		}

		public void setLinked(Settlement settlement) {
			linked = settlement;
		}

	}

    @Entity(name = "Task")
	@Table(name = "wf_task")
	@Inheritance
	public abstract static class Task<T> {

		@Id
		@GeneratedValue
		private Long id;

		@Version
		private Integer version;

		@CreationTimestamp
		private Date creationDate;

		@UpdateTimestamp
		private Date modifiedDate;

		@Temporal( TemporalType.DATE )
		private Date startDate;

		@Temporal( TemporalType.DATE )
		private Date closeDate;

		@Temporal( TemporalType.DATE )
		private Date dueDate;

		@Temporal( TemporalType.DATE )
		private Date stateDueDate;

		@Temporal( TemporalType.DATE )
		private Date statusDueDate;

		@Temporal( TemporalType.DATE )
		private Date stateTransitionDate;

		@Temporal( TemporalType.DATE )
		private Date statusTransitionDate;

		@ManyToOne(fetch = FetchType.LAZY)
		private Task<?> parent;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "task_status")
		private TaskStatus status;

		@OneToMany(mappedBy = "parent")
		private Set<Task<?>> children = new HashSet<>();

		@OneToMany(mappedBy = "status")
		private Set<Task<?>> linkedTasks = new HashSet<>();

		public abstract T getLinked();
		public abstract void setLinked(T linked);

		public void addChild(Task<?> task) {
			task.setParent(this);
			children.add( task);
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Integer getVersion() {
			return  version;
		}

		public void setVersion(Integer version) {
			this.version = version;
		}

		public Date getCreationDate() {
			return  creationDate;
		}

		public void setCreationDate(Date creationDate) {
			this.creationDate = creationDate;
		}

		public Date getModifiedDate() {
			return  modifiedDate;
		}

		public void setModifiedDate(Date modifiedDate) {
			this.modifiedDate = modifiedDate;
		}

		public TaskStatus getStatus() {
			return status;
		}

		public void setStatus(TaskStatus status) {
			this.status = status;
		}

		@SuppressWarnings("unchecked")
		public <X extends Task<?>> Set<X> getChildren(Class<X> ofType) {
			Set<X> children = null;

			children = new LinkedHashSet<X>();
			for (Task<?> child : this.children ) {
				if (ofType.isInstance(child)) {
					children.add((X) child);
				}
			}
			return children;
		}

		public Set<Task<?>> getChildren() {
			return children;
		}

		public void setChildren(Set<Task<?>> links) {
			children = links;
		}

		public Date getStartDate() {
			return  startDate;
		}

		public void setStartDate(Date openDate) {
		 	startDate = openDate;
		}

		public Date getCloseDate() {
			return  closeDate;
		}

		public void setCloseDate(Date closeDate) {
			this.closeDate = closeDate;
		}

		public Date getDueDate() {
			return  dueDate;
		}

		public void setDueDate(Date expiryDate) {
		 	dueDate = expiryDate;
		}

		public Task<?> getParent() {
			return parent;
		}

		public void setParent(Task<?> parentTask) {
			parent = parentTask;
		}

		public Set<Task<?>> getLinkedTasks() {
			return linkedTasks;
		}

		public void setLinkedTasks(Set<Task<?>> linkedTasks) {
			this.linkedTasks = linkedTasks;
		}

		public Date getStateTransitionDate() {
			return  stateTransitionDate;
		}

		public void setStateTransitionDate(Date stateTransitionDate) {
			this.stateTransitionDate = stateTransitionDate;
		}

		public Date getStatusTransitionDate() {
			return  statusTransitionDate;
		}

		public void setStatusTransitionDate(Date taskTransitionDate) {
		 	statusTransitionDate = taskTransitionDate;
		}

		public Date getStateDueDate() {
			return  stateDueDate;
		}

		public void setStateDueDate(Date stateDueDate) {
			this.stateDueDate = stateDueDate;
		}

		public Date getStatusDueDate() {
			return  statusDueDate;
		}

		public void setStatusDueDate(Date statusDueDate) {
			this.statusDueDate = statusDueDate;
		}

	}

    @Entity(name = "TaskStatus")
	@Table(name = "wf_task_status")
	public static class TaskStatus {

		@Id
		@GeneratedValue
		private Long  id;

		@Version
		private Integer  version;

		@CreationTimestamp
		private Date  creationDate;

		@UpdateTimestamp
		private Date  modifiedDate;

		private boolean  active;

		@Column(name = "order_index")
		private Integer orderIndex;

		private String  name;
		private String  displayName;

		public TaskStatus() {
		}

		public String getEntityName() {
			return  displayName;
		}

		public Long getId() {
			return  id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Integer getVersion() {
			return  version;
		}

		public void setVersion(Integer version) {
			this.version = version;
		}

		public Date getCreationDate() {
			return  creationDate;
		}

		public void setCreationDate(Date creationDate) {
			this.creationDate = creationDate;
		}

		public Date getModifiedDate() {
			return  modifiedDate;
		}

		public void setModifiedDate(Date modifiedDate) {
			this.modifiedDate = modifiedDate;
		}

		public String getName() {
			return  name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDisplayName() {
			return  displayName;
		}

		public void setDisplayName(String displayName) {
		 	this.displayName = displayName;
		}

		public boolean isActive() {
			return  active;
		}

		public void setActive(boolean active) {
			this.active = active;
		}

		@Override
		public String toString() {
			return  name;
		}

	}
}
