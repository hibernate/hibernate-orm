/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.hhh12225;

import java.util.Date;

public class Vehicle {
	public static final long serialVersionUID = 1L;
	public static final String STATUS_NEW = "new";
	public static final String STATUS_USED = "used";

	private Long _id;
	private Integer _version;
	private Date _creationDate;
	private Date _modifiedDate;

	private String _vin;
	private boolean _dirty;
	private Double _msrp;
	private Double _residualValue;
	private Double _invoicePrice;

	private Integer _decodeAttempts;
	private String _model;
	private String _modelDetail;
	private Integer _year;
	private Integer _odometer;
	private String _license;
	private String _status;
	private String _vehicleType;
	private String _classification;

	private String _country;
	private String _engineType;
	private String _assemblyPlant;
	private Integer _sequenceNumber;
	private String _bodyType;
	private String _fuelType;
	private String _driveLineType;

	private VehicleContract _contract;

	/**
	 * default constructor
	 */
	public Vehicle() {
		_decodeAttempts = 0;
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

	public String getVin() {
		return _vin;
	}

	public void setVin(String vin) {
		_vin = vin;
	}

	public boolean isDirty() {
		return _dirty;
	}

	public void setDirty(boolean dirty) {
		_dirty = dirty;
	}

	public Double getMsrp() {
		return _msrp;
	}

	public void setMsrp(Double msrp) {
		_msrp = msrp;
	}

	public Integer getDecodeAttempts() {
		return _decodeAttempts;
	}

	public void setDecodeAttempts(Integer decodeAttempts) {
		_decodeAttempts = decodeAttempts;
	}

	public String getModel() {
		return _model;
	}

	public void setModel(String model) {
		_model = model;
	}

	public String getModelDetail() {
		return _modelDetail;
	}

	public void setModelDetail(String modelDetail) {
		_modelDetail = modelDetail;
	}

	public Integer getYear() {
		return _year;
	}

	public void setYear(Integer year) {
		_year = year;
	}

	public Integer getOdometer() {
		return _odometer;
	}

	public void setOdometer(Integer odometer) {
		_odometer = odometer;
	}

	public String getLicense() {
		return _license;
	}

	public void setLicense(String license) {
		_license = license;
	}

	public String getStatus() {
		return _status;
	}

	public void setStatus(String status) {
		_status = status;
	}

	public String getVehicleType() {
		return _vehicleType;
	}

	public void setVehicleType(String vehicleType) {
		_vehicleType = vehicleType;
	}

	public String getCountry() {
		return _country;
	}

	public void setCountry(String country) {
		_country = country;
	}

	public String getEngineType() {
		return _engineType;
	}

	public void setEngineType(String engineType) {
		_engineType = engineType;
	}

	public String getAssemblyPlant() {
		return _assemblyPlant;
	}

	public void setAssemblyPlant(String assemblyPlant) {
		_assemblyPlant = assemblyPlant;
	}

	public Integer getSequenceNumber() {
		return _sequenceNumber;
	}

	public void setSequenceNumber(Integer sequenceNumber) {
		_sequenceNumber = sequenceNumber;
	}

	public String getBodyType() {
		return _bodyType;
	}

	public void setBodyType(String bodyType) {
		_bodyType = bodyType;
	}

	public String getFuelType() {
		return _fuelType;
	}

	public void setFuelType(String fuelType) {
		_fuelType = fuelType;
	}

	public String getDriveLineType() {
		return _driveLineType;
	}

	public void setDriveLineType(String driveLineType) {
		_driveLineType = driveLineType;
	}

	public VehicleContract getContract() {
		return _contract;
	}

	public void setContract(VehicleContract contract) {
		_contract = contract;
	}

	public String getClassification() {
		return _classification;
	}

	public void setClassification(String classification) {
		_classification = classification;
	}

	public Double getResidualValue() {
		return _residualValue;
	}

	public void setResidualValue(Double residualValue) {
		_residualValue = residualValue;
	}

	public Double getInvoicePrice() {
		return _invoicePrice;
	}

	public void setInvoicePrice(Double invoicePrice) {
		_invoicePrice = invoicePrice;
	}

	public String toString() {
		return String.valueOf( _vin );
	}

	public boolean equals(Object obj) {
		if ( obj == null ) {
			return false;
		}
		if ( obj == this ) {
			return true;
		}
		if ( obj instanceof Vehicle ) {
			//TODO - needs to include contract equals comparision
			return _vin.equals( ( (Vehicle) obj ).getVin() );
		}
		return false;
	}

	public int hashCode() {
		return _vin.hashCode();
	}


}
