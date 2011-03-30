package org.hibernate.metamodel.source.hbm.state.domain;

import org.dom4j.Element;

import org.hibernate.metamodel.binding.CollectionElement;
import org.hibernate.metamodel.binding.HibernateTypeDescriptor;
import org.hibernate.metamodel.source.util.DomHelper;

/**
 * Created by IntelliJ IDEA.
 * User: gbadner
 * Date: 3/29/11
 * Time: 9:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class HbmCollectionElementDomainState implements CollectionElement.DomainState {
	private final Element element;

	HbmCollectionElementDomainState(Element element) {
		this.element = element;
	}

	public final HibernateTypeDescriptor getHibernateTypeDescriptor() {
		HibernateTypeDescriptor hibernateTypeDescriptor = new HibernateTypeDescriptor();
		hibernateTypeDescriptor.setTypeName( DomHelper.extractAttributeValue( element, "type", null ) );
		return hibernateTypeDescriptor;
	}

	public final String getNodeName() {
		return DomHelper.extractAttributeValue( element, "node", null );
	}
}
