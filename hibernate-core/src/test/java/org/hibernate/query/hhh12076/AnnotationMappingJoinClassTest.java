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
		private Long _id;

		@Version
		private Integer _version;

		@CreationTimestamp
		private Date _creationDate;

		@UpdateTimestamp
		private Date _modifiedDate;

		private Long _trackingId;
		private Integer _term;
		private Double _initialReserve = 0.0;

		@Temporal( TemporalType.DATE )
		private Date _effectiveDate;

		@Temporal( TemporalType.DATE )
		private Date _expiryDate;

		@Temporal( TemporalType.DATE )
		private Date _notificationDate;

		@Temporal( TemporalType.DATE )
		private Date _pendingDate;

		@Temporal( TemporalType.DATE )
		private Date _openDate;

		@Temporal( TemporalType.DATE )
		private Date _suspendDate;

		@Temporal( TemporalType.DATE )
		private Date _closeDate;

		private String _externalId;
		private String _importRef;
		private String _location;

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
			return _id;
		}

		public void setId(Long id) {
			_id = id;
		}

		public Date getCreationDate() {
			return _creationDate;
		}

		public void setCreationDate(Date creationDate) {
			_creationDate = creationDate;
		}

		public Date getModifiedDate() {
			return _modifiedDate;
		}

		public void setModifiedDate(Date modifiedDate) {
			_modifiedDate = modifiedDate;
		}

		public Integer getVersion() {
			return _version;
		}

		public void setVersion(Integer version) {
			_version = version;
		}

		public Long getTrackingId() {
			return _trackingId;
		}

		public void setTrackingId(Long trackingId) {
			_trackingId = trackingId;
		}

		public String getExternalId() {
			return _externalId;
		}

		public void setExternalId(String externalId) {
			_externalId = externalId;
		}

		public Date getEffectiveDate() {
			return _effectiveDate;
		}

		public void setEffectiveDate(Date effectiveDate) {
			_effectiveDate = effectiveDate;
		}

		public Date getExpiryDate() {
			return _expiryDate;
		}

		public void setExpiryDate(Date expiryDate) {
			_expiryDate = expiryDate;
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
			return _notificationDate;
		}

		public void setNotificationDate(Date notificationDate) {
			_notificationDate = notificationDate;
		}

		public Date getOpenDate() {
			return _openDate;
		}

		public void setOpenDate(Date openDate) {
			_openDate = openDate;
		}

		public Date getCloseDate() {
			return _closeDate;
		}

		public void setCloseDate(Date closeDate) {
			_closeDate = closeDate;
		}

		public Double getInitialReserve() {
			return _initialReserve;
		}

		public void setInitialReserve(Double initialReserve) {
			_initialReserve = initialReserve;
		}

		public String getLocation() {
			return _location;
		}

		public void setLocation(String location) {
			_location = location;
		}

		public String getImportRef() {
			return _importRef;
		}

		public void setImportRef(String importRef) {
			_importRef = importRef;
		}

		public Date getPendingDate() {
			return _pendingDate;
		}

		public void setPendingDate(Date startDate) {
			_pendingDate = startDate;
		}

		public Date getSuspendDate() {
			return _suspendDate;
		}

		public void setSuspendDate(Date suspendDate) {
			_suspendDate = suspendDate;
		}
	}

    @Entity(name = "EwtAssessmentExtension")
	@Table(name = "claim_settlement_ext_i3_ewt")
	public static class EwtAssessmentExtension extends SettlementExtension {
		public static final long serialVersionUID = 1L;

		private Double _requestedUnits = -1.0; //2
		private Double _requestedUnitAmount = -1.0; //$150
		private Double _requestedSubtotal = 0.0; //$300
		private Double _requestedTaxAmount = 0.0; //$30
		private Double _requestedTotal = 0.0;  //$330

		private Double _coveredRatio = 0.0;
		private Double _coveredUnits = 0.0;
		private Double _coveredUnitAmount = 0.0;
		private Double _coveredUnitAmountOverride = 0.0;
		private Double _coveredSubtotal = 0.0;
		private Double _coveredTaxAmount = 0.0;
		private Double _coveredTotal = 0.0;

		private Double _underinsuredAmount = 0.0;
		private Double _shortfallUnitAmount = 0.0;
		private Double _shortfallTotal = 0.0;

		private Double _taxRate = 0.0;

		private String _details;
		private String _damageType;
		private String _exclusion;
		private Boolean _validInspection;
		private Boolean _taxExempt = false;

		public EwtAssessmentExtension() {
		}

		public Double getRequestedUnits() {
			return _requestedUnits;
		}

		public void setRequestedUnits(Double requestedUnits) {
			_requestedUnits = requestedUnits;
		}

		public Double getRequestedUnitAmount() {
			return _requestedUnitAmount;
		}

		public void setRequestedUnitAmount(Double requestedBenefitPerUnit) {
			_requestedUnitAmount = requestedBenefitPerUnit;
		}

		public Double getRequestedSubtotal() {
			return _requestedSubtotal;
		}

		public void setRequestedSubtotal(Double requestedBenefitSubtotal) {
			_requestedSubtotal = requestedBenefitSubtotal;
		}

		public Double getRequestedTaxAmount() {
			return _requestedTaxAmount;
		}

		public void setRequestedTaxAmount(Double requestedBenefitTax) {
			_requestedTaxAmount = requestedBenefitTax;
		}

		public Double getRequestedTotal() {
			return _requestedTotal;
		}

		public void setRequestedTotal(Double requestedBenefitTotal) {
			_requestedTotal = requestedBenefitTotal;
		}

		public Double getCoveredUnitAmount() {
			return _coveredUnitAmount;
		}

		public void setCoveredUnitAmount(Double coveredBenefitPerUnit) {
			_coveredUnitAmount = coveredBenefitPerUnit;
		}

		public Double getCoveredSubtotal() {
			return _coveredSubtotal;
		}

		public void setCoveredSubtotal(Double coveredBenefitSubtotal) {
			_coveredSubtotal = coveredBenefitSubtotal;
		}

		public Double getCoveredTaxAmount() {
			return _coveredTaxAmount;
		}

		public void setCoveredTaxAmount(Double coveredTaxAmount) {
			_coveredTaxAmount = coveredTaxAmount;
		}

		public Double getCoveredTotal() {
			return _coveredTotal;
		}

		public void setCoveredTotal(Double coveredBenefitTotal) {
			_coveredTotal = coveredBenefitTotal;
		}

		public Double getTaxRate() {
			return _taxRate;
		}

		public void setTaxRate(Double taxRate) {
			_taxRate = taxRate;
		}

		public Double getShortfallUnitAmount() {
			return _shortfallUnitAmount;
		}

		public void setShortfallUnitAmount(Double shortfallUnitAmount) {
			_shortfallUnitAmount = shortfallUnitAmount;
		}

		public Double getShortfallTotal() {
			return _shortfallTotal;
		}

		public void setShortfallTotal(Double shortfallTotal) {
			_shortfallTotal = shortfallTotal;
		}

		public String getDetails() {
			return _details;
		}

		public void setDetails(String description) {
			_details = description;
		}

		public Double getUnderinsuredAmount() {
			return _underinsuredAmount;
		}

		public void setUnderinsuredAmount(Double truncatedAmount) {
			_underinsuredAmount = truncatedAmount;
		}

		public Double getCoveredUnits() {
			return _coveredUnits;
		}

		public void setCoveredUnits(Double coveredUnits) {
			_coveredUnits = coveredUnits;
		}

		public String getDamageType() {
			return _damageType;
		}

		public void setDamageType(String damageType) {
			_damageType = damageType;
		}

		public Double getCoveredRatio() {
			return _coveredRatio;
		}

		public void setCoveredRatio(Double coveredRatio) {
			_coveredRatio = coveredRatio;
		}

		public String getExclusion() {
			return _exclusion;
		}

		public void setExclusion(String exclusion) {
			_exclusion = exclusion;
		}

		public Double getCoveredUnitAmountOverride() {
			return _coveredUnitAmountOverride;
		}

		public void setCoveredUnitAmountOverride(Double coveredUnitOverride) {
			_coveredUnitAmountOverride = coveredUnitOverride;
		}

		public Boolean isValidInspection() {
			return _validInspection;
		}

		public void setValidInspection(Boolean validInspection) {
			_validInspection = validInspection;
		}

		public Boolean isTaxExempt() {
			return _taxExempt;
		}

		public void setTaxExempt(Boolean taxExempt) {
			_taxExempt = taxExempt;
		}

	}

    @Entity(name = "Extension")
	@Table(name = "claim_ext")
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
			modifiedDate = modifiedDate;
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

		private Double _insuredsObligation = 0.0;
		private Double _eligibleAmount = 0.0;
		private Double _assessedAmount = 0.0;
		private Double _underinsuredAmount = 0.0;

		public Double getAssessedAmount() {
			return _assessedAmount;
		}

		public void setAssessedAmount(Double assessedAmount) {
			_assessedAmount = assessedAmount;
		}

		public Double getEligibleAmount() {
			return _eligibleAmount;
		}

		public void setEligibleAmount(Double eligible) {
			_eligibleAmount = eligible;
		}

		public Double getUnderinsuredAmount() {
			return _underinsuredAmount;
		}

		public void setUnderinsuredAmount(Double underinsuredAmount) {
			_underinsuredAmount = underinsuredAmount;
		}

		public Double getInsuredsObligation() {
			return _insuredsObligation;
		}

		public void setInsuredsObligation(Double insuredsObligation) {
			_insuredsObligation = insuredsObligation;
		}
	}

    @Entity(name = "Settlement")
	@Table(name = "claim_settlement")
	public static class Settlement {

		@Id
		@GeneratedValue
		private Long _id;

		@Version
		private Integer _version;

		@CreationTimestamp
		private Date _creationDate;

		@UpdateTimestamp
		private Date _modifiedDate;

		private Boolean _override = false;
		private Boolean _started = false;
		private Boolean _taxable = false;

		private Double _units = 0.0;
		private Double _amount = 0.0;
		private Double _subtotal = 0.0;

		private Double _taxRate = 0.0;
		private Double _taxAmount = 0.0;

		private Double _goodwill = 0.0;
		private Double _totalAmount = 0.0;
		private Double _underinsuredAmount = 0.0;

		@Temporal( TemporalType.TIMESTAMP )
		private Date _openDate;

		@Temporal( TemporalType.TIMESTAMP )
		private Date _allocateDate;

		@Temporal( TemporalType.TIMESTAMP )
		private Date _closeDate;

		private String _trackingId;

		@ManyToOne(fetch = FetchType.LAZY)
		private Claim claim;

		@Enumerated(EnumType.STRING)
		private SettlementStatus status = SettlementStatus.RESERVED;

		@OneToMany(mappedBy = "settlement", cascade = CascadeType.ALL, orphanRemoval = true)
		@OrderColumn(name = "order_index")
		private Set<SettlementExtension> extensions = new HashSet<>();

		private transient Map<Class<?>, SettlementExtension> _extensionMap;

		public Long getId() {
			return _id;
		}

		protected void setId(Long id) {
			_id = id;
		}

		public Integer getVersion() {
			return _version;
		}

		public void setVersion(Integer version) {
			_version = version;
		}

		public Date getCreationDate() {
			return _creationDate;
		}

		public void setCreationDate(Date creationDate) {
			_creationDate = creationDate;
		}

		public Date getModifiedDate() {
			return _modifiedDate;
		}

		public void setModifiedDate(Date modifiedDate) {
			_modifiedDate = modifiedDate;
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
			return _trackingId;
		}

		public void setTrackingId(String trackingId) {
			_trackingId = trackingId;
		}

		public Double getUnits() {
			return _units;
		}

		public void setUnits(Double units) {
			_units = units;
		}

		public Double getAmount() {
			return _amount;
		}

		public void setAmount(Double amount) {
			_amount = amount;
		}

		public Double getTotalAmount() {
			return _totalAmount;
		}

		public void setTotalAmount(Double totalAmount) {
			_totalAmount = totalAmount;
		}

		public Date getCloseDate() {
			return _closeDate;
		}

		public void setCloseDate(Date settlementDate) {
			_closeDate = settlementDate;
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
			if (_extensionMap == null || _extensionMap.size() != extensions.size()) {
				Map<Class<?>, SettlementExtension> map = new HashMap<Class<?>, SettlementExtension>( extensions.size());
				for (SettlementExtension extension : extensions ) {
					map.put(extension.getClass(), extension);
				}
				_extensionMap = map;
			}
			return (X)_extensionMap.get(extensionType);
		}

		public <X extends SettlementExtension> boolean hasExtension(Class<X> extensionType) {
			return getExtension(extensionType) != null;
		}

		public Boolean isOverride() {
			return _override;
		}

		public void setOverride(Boolean override) {
			_override = override;
		}

		public Double getGoodwill() {
			return _goodwill;
		}

		public void setGoodwill(Double goodwill) {
			_goodwill = goodwill;
		}

		public Date getOpenDate() {
			return _openDate;
		}

		public void setOpenDate(Date startDate) {
			_openDate = startDate;
		}

		public Date getAllocateDate() {
			return _allocateDate;
		}

		public void setAllocateDate(Date allocateDate) {
			_allocateDate = allocateDate;
		}

		public Double getSubtotal() {
			return _subtotal;
		}

		public void setSubtotal(Double subtotal) {
			_subtotal = subtotal;
		}

		public Double getTaxRate() {
			return _taxRate;
		}

		public void setTaxRate(Double taxRate) {
			_taxRate = taxRate;
		}

		public Double getTaxAmount() {
			return _taxAmount;
		}

		public void setTaxAmount(Double taxAmount) {
			_taxAmount = taxAmount;
		}

		public Double getUnderinsuredAmount() {
			return _underinsuredAmount;
		}

		public void setUnderinsuredAmount(Double underinsuredAmount) {
			_underinsuredAmount = underinsuredAmount;
		}

		public Boolean isStarted() {
			return _started;
		}

		public void setStarted(Boolean started) {
			_started = started;
		}

		public Boolean isTaxable() {
			return _taxable;
		}

		public void setTaxable(Boolean taxable) {
			_taxable = taxable;
		}

	}

    @Entity(name = "SettlementExtension")
	@Table(name = "claim_settlement_ext")
	public abstract static class SettlementExtension {

		@Id
		@GeneratedValue
		private Long _id;

		@Version
		private Integer _version;

		@CreationTimestamp
		private Date _creationDate;

		@UpdateTimestamp
		private Date _modifiedDate;

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
			return _id;
		}

		protected void setId(Long id) {
			_id = id;
		}

		public Integer getVersion() {
			return _version;
		}

		public void setVersion(Integer version) {
			_version = version;
		}

		public Date getCreationDate() {
			return _creationDate;
		}

		public void setCreationDate(Date creationDate) {
			_creationDate = creationDate;
		}

		public Date getModifiedDate() {
			return _modifiedDate;
		}

		public void setModifiedDate(Date modifiedDate) {
			_modifiedDate = modifiedDate;
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
		private Long _id;

		@Version
		private Integer _version;

		@CreationTimestamp
		private Date _creationDate;

		@UpdateTimestamp
		private Date _modifiedDate;

		@Temporal( TemporalType.DATE )
		private Date _startDate;

		@Temporal( TemporalType.DATE )
		private Date _closeDate;

		@Temporal( TemporalType.DATE )
		private Date _dueDate;

		@Temporal( TemporalType.DATE )
		private Date _stateDueDate;

		@Temporal( TemporalType.DATE )
		private Date _statusDueDate;

		@Temporal( TemporalType.DATE )
		private Date _stateTransitionDate;

		@Temporal( TemporalType.DATE )
		private Date _statusTransitionDate;

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
			return _id;
		}

		public void setId(Long id) {
			_id = id;
		}

		public Integer getVersion() {
			return _version;
		}

		public void setVersion(Integer version) {
			_version = version;
		}

		public Date getCreationDate() {
			return _creationDate;
		}

		public void setCreationDate(Date creationDate) {
			_creationDate = creationDate;
		}

		public Date getModifiedDate() {
			return _modifiedDate;
		}

		public void setModifiedDate(Date modifiedDate) {
			_modifiedDate = modifiedDate;
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
			return _startDate;
		}

		public void setStartDate(Date openDate) {
			_startDate = openDate;
		}

		public Date getCloseDate() {
			return _closeDate;
		}

		public void setCloseDate(Date closeDate) {
			_closeDate = closeDate;
		}

		public Date getDueDate() {
			return _dueDate;
		}

		public void setDueDate(Date expiryDate) {
			_dueDate = expiryDate;
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
			return _stateTransitionDate;
		}

		public void setStateTransitionDate(Date stateTransitionDate) {
			_stateTransitionDate = stateTransitionDate;
		}

		public Date getStatusTransitionDate() {
			return _statusTransitionDate;
		}

		public void setStatusTransitionDate(Date taskTransitionDate) {
			_statusTransitionDate = taskTransitionDate;
		}

		public Date getStateDueDate() {
			return _stateDueDate;
		}

		public void setStateDueDate(Date stateDueDate) {
			_stateDueDate = stateDueDate;
		}

		public Date getStatusDueDate() {
			return _statusDueDate;
		}

		public void setStatusDueDate(Date statusDueDate) {
			_statusDueDate = statusDueDate;
		}

	}

    @Entity(name = "TaskStatus")
	@Table(name = "wf_task_status")
	public static class TaskStatus {

		@Id
		@GeneratedValue
		private Long _id;

		@Version
		private Integer _version;

		@CreationTimestamp
		private Date _creationDate;

		@UpdateTimestamp
		private Date _modifiedDate;

		private boolean _active;

		@Column(name = "order_index")
		private Integer orderIndex;

		private String _name;
		private String _displayName;

		public TaskStatus() {
		}

		public String getEntityName() {
			return _displayName;
		}

		public Long getId() {
			return _id;
		}

		public void setId(Long id) {
			_id = id;
		}

		public Integer getVersion() {
			return _version;
		}

		public void setVersion(Integer version) {
			_version = version;
		}

		public Date getCreationDate() {
			return _creationDate;
		}

		public void setCreationDate(Date creationDate) {
			_creationDate = creationDate;
		}

		public Date getModifiedDate() {
			return _modifiedDate;
		}

		public void setModifiedDate(Date modifiedDate) {
			_modifiedDate = modifiedDate;
		}

		public String getName() {
			return _name;
		}

		public void setName(String name) {
			_name = name;
		}

		public String getDisplayName() {
			return _displayName;
		}

		public void setDisplayName(String displayName) {
			_displayName = displayName;
		}

		public boolean isActive() {
			return _active;
		}

		public void setActive(boolean active) {
			_active = active;
		}

		@Override
		public String toString() {
			return _name;
		}

		public Integer getOrderIndex() {
			return orderIndex;
		}

		public void setOrderIndex(Integer ordering) {
			orderIndex = ordering;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((_name == null) ? 0 : _name.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null) return false;
			if (obj == this) return true;
			if (!(obj instanceof TaskStatus )) {
				return false;
			}
			TaskStatus other = (TaskStatus) obj;
			if (_name == null) {
				if (other._name != null) {
					return false;
				}
			} else if (!_name.equals(other._name)) {
				return false;
			}
			return true;
		}

	}
}
