package org.hibernate.metamodel.internal.source.annotations.xml.mocker;

import java.util.List;

import org.hibernate.jaxb.spi.orm.JaxbBasic;
import org.hibernate.jaxb.spi.orm.JaxbElementCollection;
import org.hibernate.jaxb.spi.orm.JaxbEmbedded;
import org.hibernate.jaxb.spi.orm.JaxbManyToMany;
import org.hibernate.jaxb.spi.orm.JaxbManyToOne;
import org.hibernate.jaxb.spi.orm.JaxbOneToMany;
import org.hibernate.jaxb.spi.orm.JaxbOneToOne;
import org.hibernate.jaxb.spi.orm.JaxbTransient;

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
