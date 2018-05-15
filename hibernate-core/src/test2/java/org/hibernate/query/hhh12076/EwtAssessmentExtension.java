/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.hhh12076;

public class EwtAssessmentExtension extends SettlementExtension {
	public static final long serialVersionUID = 1L;
	public static final String PROPERTY_DETAIL_OPTIONS = "detail_options";
	public static final String PROPERTY_DAMAGE_TYPE_OPTIONS = "damage_type_options";
	public static final String PROPERTY_EXCLUSION_OPTIONS = "exclusion_options";
	public static final String PROPERTY_OVERRIDE_ENABLED = "override_enabled";

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
