/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.generatedkeys.seqidentity;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Oracle9iDialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@TestForIssue( jiraKey = "HHH-13365" )
@RequiresDialect( Oracle9iDialect.class )
public class JoinedSequenceIdentityBatchTest extends BaseNonConfigCoreFunctionalTestCase {

	@Test
	public void testInsertAndUpdate() {
		doInHibernate(
				this::sessionFactory,
				session -> {
					FolderResource folder = new FolderResource();
					folder.name = "PARENT";
					session.persist( folder );
				}
		);

		doInHibernate(
				this::sessionFactory,
				session -> {
					List<FolderResource> folderResources = session.createQuery( "from FolderResource" ).getResultList();
					assertEquals( 1, folderResources.size() );
					final FolderResource folderResource = folderResources.get( 0 );
					assertNull( folderResource.description );
					folderResource.description = "A folder resource";
			}
		);

		doInHibernate(
				this::sessionFactory,
				session -> {
					List<FolderResource> folderResources = session.createQuery( "from FolderResource" ).getResultList();
					assertEquals( 1, folderResources.size() );
					final FolderResource folderResource = folderResources.get( 0 );
					assertEquals( "A folder resource", folderResource.description );
				}
		);
	}

	@Override
	@SuppressWarnings( "unchecked" )
	protected void addSettings(Map settings) {
		super.addSettings( settings );
		settings.put( AvailableSettings.STATEMENT_BATCH_SIZE, "5" );
		settings.put( AvailableSettings.USE_GET_GENERATED_KEYS, "true" );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Resource.class, FolderResource.class };
	}

	@Entity(name = "Resource")
	@Inheritance(strategy = InheritanceType.JOINED)
	@Table(name = "WORKSPACE_RESOURCE")
	@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
	public static class Resource {

		@Id
    		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_WORKSPACE_RESOURCE")
    		@GenericGenerator(name = "SEQ_WORKSPACE_RESOURCE", strategy = "sequence-identity",
					parameters = {@Parameter(name = "sequence", value = "SEQ_WORKSPACE_RESOURCE")})
		@Column(name = "ID_WORKSPACE_RESOURCE", nullable = false, precision = 18)
		private Long id;

		@Column(name = "NAME", nullable = false, length = 256)
		protected String name;

		@Column(name = "RESOURCE_TYPE", nullable = false, length = 20)
		protected String type;
	}

	@Entity(name = "FolderResource")
	@Table(name = "WORKSPACE_RESOURCE_FOLDER")
	@PrimaryKeyJoinColumn(name = "ID_WORKSPACE_RESOURCE_FOLDER")
	@DiscriminatorValue("FOLDER")
	public static class FolderResource extends Resource implements Serializable {
		private String description;

		public FolderResource()
		{
			super();
			type = "FOLDER";
		}
	}
}