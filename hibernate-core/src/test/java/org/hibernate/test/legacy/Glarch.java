/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Glarch.java 4599 2004-09-26 05:18:27Z oneovthafew $
package org.hibernate.test.legacy;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.CallbackException;
import org.hibernate.Session;
import org.hibernate.classic.Lifecycle;

public class Glarch extends Super implements GlarchProxy, Lifecycle, Named, Serializable {

	private int version;
	private GlarchProxy next;
	private short order;
	private List strings;
	private Map stringSets;
	private List fooComponents;
	private GlarchProxy[] proxyArray;
	private Set proxySet;
	//private Currency currency = Currency.getInstance( Locale.getDefault() );
	private transient Map dynaBean;
	private String immutable;
	private int derivedVersion;
	private Object any;
	private int x;
	private Multiplicity multiple;

	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}

	@Override
	public int getVersion() {
		return version;
	}

	@Override
	public void setVersion(int version) {
		this.version = version;
	}

	@Override
	public GlarchProxy getNext() {
		return next;
	}
	@Override
	public void setNext(GlarchProxy next) {
		this.next = next;
	}

	@Override
	public short getOrder() {
		return order;
	}
	@Override
	public void setOrder(short order) {
		this.order = order;
	}

	@Override
	public List getStrings() {
		return strings;
	}

	@Override
	public void setStrings(List strings) {
		this.strings = strings;
	}

	@Override
	public Map getStringSets() {
		return stringSets;
	}

	@Override
	public void setStringSets(Map stringSets) {
		this.stringSets = stringSets;
	}

	@Override
	public List getFooComponents() {
		return fooComponents;
	}

	@Override
	public void setFooComponents(List fooComponents) {
		this.fooComponents = fooComponents;
	}

	@Override
	public GlarchProxy[] getProxyArray() {
		return proxyArray;
	}
	@Override
	public void setProxyArray(GlarchProxy[] proxyArray) {
		this.proxyArray = proxyArray;
	}
	@Override
	public Set getProxySet() {
		return proxySet;
	}

	@Override
	public void setProxySet(Set proxySet) {
		this.proxySet = proxySet;
	}

	@Override
	public boolean onDelete(Session s) throws CallbackException {
		return NO_VETO;
	}

	@Override
	public void onLoad(Session s, Serializable id) {
		if ( ! ( ( (String) id ).length()==32 ) ) throw new RuntimeException("id problem");
	}

	@Override
	public boolean onSave(Session s) throws CallbackException {
		dynaBean = new HashMap();
		dynaBean.put("foo", "foo");
		dynaBean.put("bar", new Integer(66));
		immutable="never changes!";
		return NO_VETO;
	}

	@Override
	public boolean onUpdate(Session s) throws CallbackException {
		return NO_VETO;
	}

	/*public Currency getCurrency() {
		return currency;
	}

	public void setCurrency(Currency currency) {
		this.currency = currency;
	}*/

	/**
	 * Returns the dynaBean.
	 * @return DynaBean
	 */
	@Override
	public Map getDynaBean() {
		return dynaBean;
	}

	/**
	 * Sets the dynaBean.
	 * @param dynaBean The dynaBean to set
	 */
	@Override
	public void setDynaBean(Map dynaBean) {
		this.dynaBean = dynaBean;
	}

	/**
	 * Returns the immutable.
	 * @return String
	 */
	public String getImmutable() {
		return immutable;
	}

	/**
	 * Sets the immutable.
	 * @param immutable The immutable to set
	 */
	public void setImmutable(String immutable) {
		this.immutable = immutable;
	}

	/**
	 * Returns the derivedVersion.
	 * @return int
	 */
	@Override
	public int getDerivedVersion() {
		return derivedVersion;
	}

	/**
	 * Sets the derivedVersion.
	 * @param derivedVersion The derivedVersion to set
	 */
	public void setDerivedVersion(int derivedVersion) {
		this.derivedVersion = derivedVersion;
	}

	/**
	 * Returns the any.
	 * @return Object
	 */
	@Override
	public Object getAny() {
		return any;
	}

	/**
	 * Sets the any.
	 * @param any The any to set
	 */
	@Override
	public void setAny(Object any) {
		this.any = any;
	}

	/**
	 * @return
	 */
	@Override
	public Multiplicity getMultiple() {
		return multiple;
	}

	/**
	 * @param multiplicity
	 */
	@Override
	public void setMultiple(Multiplicity multiplicity) {
		multiple = multiplicity;
	}

	@Override
	public String getName() {
		return super.getName();
	}

}







