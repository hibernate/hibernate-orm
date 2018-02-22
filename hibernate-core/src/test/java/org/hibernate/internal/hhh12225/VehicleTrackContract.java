package org.hibernate.internal.hhh12225;



/** 
 *       Walkaway Contract
 *     
*/
public class VehicleTrackContract extends VehicleContract {
	public static final long serialVersionUID = 1L;
	private String _etchingId = null;
	private boolean _original = false;
	
	public String getEtchingId() {
		return _etchingId;
	}
	public void setEtchingId(String etchingId) {
		_etchingId = etchingId;
	}
	public boolean isOriginal() {
		return _original;
	}
	public void setOriginal(boolean original) {
		_original = original;
	}

}
