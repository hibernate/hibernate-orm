/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Multi.java 4599 2004-09-26 05:18:27Z oneovthafew $
package org.hibernate.test.legacy;
import java.util.Calendar;


public class Multi extends Top {
	
	/**
	 * Constructor for Multi.
	 */
	public Multi() {
		super();
	}
	
	private String extraProp;
	private String derived;
	private Component comp;
	private Po po;
	
	/**
	 * Returns the extraProp.
	 * @return String
	 */
	public String getExtraProp() {
		return extraProp;
	}
	
	/**
	 * Sets the extraProp.
	 * @param extraProp The extraProp to set
	 */
	public void setExtraProp(String extraProp) {
		this.extraProp = extraProp;
	}
	
	public static final class Component {
		private Calendar cal;
		private Float floaty;
		/**
		 * Returns the cal.
		 * @return Calendar
		 */
		public Calendar getCal() {
			return cal;
		}
		
		/**
		 * Sets the cal.
		 * @param cal The cal to set
		 */
		public void setCal(Calendar cal) {
			this.cal = cal;
		}
		
		/**
		 * Returns the floaty.
		 * @return Float
		 */
		public Float getFloaty() {
			return floaty;
		}
		
		/**
		 * Sets the floaty.
		 * @param floaty The floaty to set
		 */
		public void setFloaty(Float floaty) {
			this.floaty = floaty;
		}
		
	}
	
	/**
	 * Returns the comp.
	 * @return Component
	 */
	public Component getComp() {
		return comp;
	}
	
	/**
	 * Sets the comp.
	 * @param comp The comp to set
	 */
	public void setComp(Component comp) {
		this.comp = comp;
	}
	
	/**
	 * Returns the po.
	 * @return Po
	 */
	public Po getPo() {
		return po;
	}
	
	/**
	 * Sets the po.
	 * @param po The po to set
	 */
	public void setPo(Po po) {
		this.po = po;
	}
	
	public String getDerived() {
		return derived;
	}

	public void setDerived(String derived) {
		this.derived = derived;
	}

}






