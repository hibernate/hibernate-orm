package org.hibernate.internal.hhh12225;

import java.util.Date;


/** 
 *       Walkaway Contract
 *     
*/
public class Contract {
	public static final long serialVersionUID = 1L;
	
	//a transient flag used to override some validation rule processing
	//TODO - not the best implementation
	private transient boolean overrideEnabled = false;
	
    private Long id;
	private Date creationDate;
	private Date modifiedDate;
    private Integer version;

    private boolean rendered;
    private boolean fixedPrice;
    private boolean renewable;
    private boolean emailDistributionRequested;

	private Integer financedTerm;
	private Integer financedAmortizationPeriod;
	private Integer coverageTerm;

    private Long trackingId;
    private Long timeToCreate = 0L;
    private Long templateId;

    private Double productPrice = 0.0;
    private Double totalCost = 0.0;
    private Double paymentAmount = 0.0;
    private Double price = 0.0;
	private Double financedAmount = 0.0;
	private Double coverageBenefit = 0.0;
	private Double coveragePaymentRelief = 0.0;
	private Double coverageFinanced = 0.0;
	private Double previousDeficiency = 0.0;
	private Double coverageDeficiency = 0.0;
	private Double interestRate = 0.0;

    private String externalId;
    private String paymentMethod;
    private String paymentFrequency;
	private String accountNumber;
	private String origin;
    private String premiumFinanced;
    private String locale;
    
    private Date effectiveDate;
    private Date terminationDate;
    private Date renewalDate;
    private Date expiryDate;

    private Contract _endorsed;

    /** default constructor */
    public Contract() {
    }

    /**
     * Returns true if contract has not yet been assigned an id
     * @return
     */
    public boolean isNew() {
    	return this.id == null;
    }
    
    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

	public Date getEffectiveDate() {
        return this.effectiveDate;
    }

    public void setEffectiveDate(Date effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public Date getTerminationDate() {
        return this.terminationDate;
    }

    public void setTerminationDate(Date terminationDate) {
        this.terminationDate = terminationDate;
    }

    public String getExternalId() {
        return this.externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public Long getTemplateId() {
        return this.templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public Double getPrice() {
        return this.price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getVersion() {
        return this.version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String toString() {
    	return String.valueOf(id);
    }
    
	/**
	 * @return
	 */
	public Integer getFinancedTerm() {
		return financedTerm;
	}

	/**
	 * @param integer
	 */
	public void setFinancedTerm(Integer integer) {
		financedTerm = integer;
	}

	/**
	 * @return
	 */
	public Long getTrackingId() {
		return trackingId;
	}

	/**
	 * @param id
	 */
	public void setTrackingId(Long id) {
		trackingId = id;
	}

	/**
	 * @return
	 */
	public Date getCreationDate() {
		return creationDate;
	}

	/**
	 * @return
	 */
	public Date getModifiedDate() {
		return modifiedDate;
	}

	/**
	 * @param date
	 */
	public void setCreationDate(Date date) {
		creationDate = date;
	}

	/**
	 * @param date
	 */
	public void setModifiedDate(Date date) {
		modifiedDate = date;
	}

	/**
	 * @return
	 */
	public Integer getCoverageTerm() {
		return coverageTerm;
	}

	/**
	 * @param integer
	 */
	public void setCoverageTerm(Integer integer) {
		coverageTerm = integer;
	}

	/**
	 * @return
	 */
	public String getAccountNumber() {
		return accountNumber;
	}

	/**
	 * @return
	 */
	public Double getFinancedAmount() {
		return financedAmount;
	}

	/**
	 * @param string
	 */
	public void setAccountNumber(String string) {
		accountNumber = string;
	}

	/**
	 * @param double1
	 */
	public void setFinancedAmount(Double double1) {
		financedAmount = double1;
	}

	/**
	 * @return
	 */
	public Double getCoverageDeficiency() {
		return coverageDeficiency;
	}

	/**
	 * @return
	 */
	public Double getPreviousDeficiency() {
		return previousDeficiency;
	}

	/**
	 * @param double1
	 */
	public void setCoverageDeficiency(Double double1) {
		coverageDeficiency = double1;
	}

	/**
	 * @param double1
	 */
	public void setPreviousDeficiency(Double double1) {
		previousDeficiency = double1;
	}

	/**
	 * @return
	 */
	public Double getTotalCost() {
		return totalCost;
	}

	/**
	 * @param double1
	 */
	public void setTotalCost(Double double1) {
		totalCost = double1;
	}

	/**
	 * @return
	 */
	public Double getInterestRate() {
		return interestRate;
	}

	/**
	 * @param double1
	 */
	public void setInterestRate(Double double1) {
		interestRate = double1;
	}

	/**
	 * @return
	 */
	public boolean isRendered() {
		return rendered;
	}

	/**
	 * @param b
	 */
	public void setRendered(boolean b) {
		rendered = b;
	}

	/**
	 * @return
	 */
	public Double getCoverageBenefit() {
		return coverageBenefit;
	}

	/**
	 * @return
	 */
	public Double getCoverageFinanced() {
		return coverageFinanced;
	}

	/**
	 * @param double1
	 */
	public void setCoverageBenefit(Double double1) {
		coverageBenefit = double1;
	}

	/**
	 * @param double1
	 */
	public void setCoverageFinanced(Double double1) {
		coverageFinanced = double1;
	}

	public boolean isFixedPrice() {
		return fixedPrice;
	}

	public void setFixedPrice(boolean fixedPrice) {
		this.fixedPrice = fixedPrice;
	}

	public Double getProductPrice() {
		return productPrice;
	}

	public void setProductPrice(Double productPrice) {
		this.productPrice = productPrice;
	}

	public Long getTimeToCreate() {
		return timeToCreate;
	}

	public void setTimeToCreate(Long timeToCreate) {
		this.timeToCreate = timeToCreate;
	}

	public String getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public String getPaymentFrequency() {
		return paymentFrequency;
	}

	public void setPaymentFrequency(String paymentFrequency) {
		this.paymentFrequency = paymentFrequency;
	}

	public Double getPaymentAmount() {
		return paymentAmount;
	}

	public void setPaymentAmount(Double paymentAmount) {
		this.paymentAmount = paymentAmount;
	}

	public Double getCoveragePaymentRelief() {
		return coveragePaymentRelief;
	}

	public void setCoveragePaymentRelief(Double coveragePaymentRelief) {
		this.coveragePaymentRelief = coveragePaymentRelief;
	}

	public Date getExpiryDate() {
		return expiryDate;
	}

	public void setExpiryDate(Date coverageTerminationDate) {
		this.expiryDate = coverageTerminationDate;
	}

	public String getPremiumFinanced() {
		return premiumFinanced;
	}

	public void setPremiumFinanced(String premiumFinanced) {
		this.premiumFinanced = premiumFinanced;
	}

	public Integer getFinancedAmortizationPeriod() {
		return financedAmortizationPeriod;
	}

	public void setFinancedAmortizationPeriod(Integer financedAmortizationPeriod) {
		this.financedAmortizationPeriod = financedAmortizationPeriod;
	}

	public Date getRenewalDate() {
		return renewalDate;
	}

	public void setRenewalDate(Date refinancingDate) {
		this.renewalDate = refinancingDate;
	}

	public boolean isRenewable() {
		return renewable;
	}

	public void setRenewable(boolean renewable) {
		this.renewable = renewable;
	}

	public boolean isEmailDistributionRequested() {
		return emailDistributionRequested;
	}

	public void setEmailDistributionRequested(boolean emailDistributionRequested) {
		this.emailDistributionRequested = emailDistributionRequested;
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(String origin) {
		this.origin = origin;
	}

	public Contract getEndorsed() {
		return _endorsed;
	}

	public void setEndorsed(Contract endorsed) {
		_endorsed = endorsed;
	}
	
	public boolean isEndorsement() {
		return _endorsed != null;
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	public boolean isOverrideEnabled() {
		return overrideEnabled;
	}

	public void setOverrideEnabled(boolean overrideEnabled) {
		this.overrideEnabled = overrideEnabled;
	}
	
}
