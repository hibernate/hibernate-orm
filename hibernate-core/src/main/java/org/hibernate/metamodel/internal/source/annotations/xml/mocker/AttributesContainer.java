package org.hibernate.metamodel.internal.source.annotations.xml.mocker;

import java.util.List;

import org.hibernate.internal.jaxb.mapping.orm.JaxbBasic;
import org.hibernate.internal.jaxb.mapping.orm.JaxbElementCollection;
import org.hibernate.internal.jaxb.mapping.orm.JaxbEmbedded;
import org.hibernate.internal.jaxb.mapping.orm.JaxbManyToMany;
import org.hibernate.internal.jaxb.mapping.orm.JaxbManyToOne;
import org.hibernate.internal.jaxb.mapping.orm.JaxbOneToMany;
import org.hibernate.internal.jaxb.mapping.orm.JaxbOneToOne;
import org.hibernate.internal.jaxb.mapping.orm.JaxbTransient;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public interface AttributesContainer {

	 List<JaxbTransient> getTransient();

	 List<JaxbBasic> getBasic();

	 List<JaxbElementCollection> getElementCollection();

	 List<JaxbEmbedded> getEmbedded();

	 List<JaxbManyToMany> getManyToMany();

	 List<JaxbManyToOne> getManyToOne();

	 List<JaxbOneToMany> getOneToMany();

	 List<JaxbOneToOne> getOneToOne();

}
