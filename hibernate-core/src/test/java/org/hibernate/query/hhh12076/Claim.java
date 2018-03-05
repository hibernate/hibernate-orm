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

public class Claim {
	public static final long serialVersionUID = 1L;

	private Long _id;
	private Date _creationDate;
	private Date _modifiedDate;
	private Integer _version;

	private Long _trackingId;
	private Integer _term;
	private Double _initialReserve = 0.0;

	private Date _effectiveDate;
	private Date _expiryDate;

	private Date _notificationDate;
	private Date _pendingDate;
	private Date _openDate;
	private Date _suspendDate;
	private Date _closeDate;

	private String _externalId;
	private String _importRef;
	private String _location;

	private Set<Extension> _extensions;
	private Set<Settlement> _settlements;

	private transient volatile Map<Class<?>, Extension> _extensionMap;

	/**
	 * default constructor
	 */
	public Claim() {
		_extensions = new HashSet<Extension>();
		_settlements = new HashSet<Settlement>();
	}

	public Claim getClaim() {
		return this;
	}

	public void addExtension(Extension extension) {
		_extensions.add( extension );
		extension.setClaim( this );
	}

	public void addSettlement(Settlement settlement) {
		_settlements.add( settlement );
		settlement.setClaim( this );
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
		return _extensions;
	}

	public void setExtensions(Set<Extension> extensions) {
		_extensions = extensions;
	}

	public Set<Settlement> getSettlements() {
		return _settlements;
	}

	public void setSettlements(Set<Settlement> settlements) {
		_settlements = settlements;
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

	@SuppressWarnings("unchecked")
	public <X extends Extension> X getExtension(Class<X> extensionType) {
		if ( _extensionMap == null || _extensionMap.size() != _extensions.size() ) {
			Map<Class<?>, Extension> map = new HashMap<Class<?>, Extension>( _extensions.size() );
			map = new HashMap<Class<?>, Extension>( _extensions.size() );
			for ( Extension extension : _extensions ) {
				map.put( extension.getClass(), extension );
			}
			_extensionMap = map;
		}
		return (X) _extensionMap.get( extensionType );
	}
}
