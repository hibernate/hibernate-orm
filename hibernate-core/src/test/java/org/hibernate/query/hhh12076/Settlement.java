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
import java.util.Map;
import java.util.Set;

public class Settlement {

	private Long _id;
	private Integer _version;
	private Date _creationDate;
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

	private Date _openDate;
	private Date _allocateDate;
	private Date _closeDate;

	private String _trackingId;

	private Claim _claim;
	private SettlementStatus _status = SettlementStatus.RESERVED;

	private Set<SettlementExtension> _extensions;

	private transient Map<Class<?>, SettlementExtension> _extensionMap;

	public Settlement() {
		_extensions = new HashSet<SettlementExtension>();
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

	public Claim getClaim() {
		return _claim;
	}

	public void setClaim(Claim claim) {
		_claim = claim;
	}

	public SettlementStatus getStatus() {
		return _status;
	}

	public void setStatus(SettlementStatus status) {
		_status = status;
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
		return _extensions;
	}

	public void setExtensions(Set<SettlementExtension> extensions) {
		_extensions = extensions;
	}

	public void addExtension(SettlementExtension extension) {
		if ( !hasExtension( extension.getClass() ) ) {
			if ( extension.getOrderIndex() == null ) {
				extension.setOrderIndex( _extensions.size() );
			}
			extension.setSettlement( this );
			_extensions.add( extension );
		}
	}

	@SuppressWarnings("unchecked")
	public <X extends SettlementExtension> X getExtension(Class<X> extensionType) {
		if ( _extensionMap == null || _extensionMap.size() != _extensions.size() ) {
			Map<Class<?>, SettlementExtension> map = new HashMap<Class<?>, SettlementExtension>( _extensions.size() );
			for ( SettlementExtension extension : _extensions ) {
				map.put( extension.getClass(), extension );
			}
			_extensionMap = map;
		}
		return (X) _extensionMap.get( extensionType );
	}

	public <X extends SettlementExtension> boolean hasExtension(Class<X> extensionType) {
		return getExtension( extensionType ) != null;
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
