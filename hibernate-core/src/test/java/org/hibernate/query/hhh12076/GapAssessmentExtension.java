/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.hhh12076;

public class GapAssessmentExtension extends SettlementExtension {
	public static final long serialVersionUID = 1L;

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
