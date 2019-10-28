/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy.inlinedirtychecking;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.NaturalId;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
@RunWith(BytecodeEnhancerRunner.class)
@CustomEnhancementContext({ DirtyCheckEnhancementContext.class, NoDirtyCheckEnhancementContext.class })
@TestForIssue(jiraKey = "HHH-13705")
public class ManyToOnePropertyAccessByFieldTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.applySetting( AvailableSettings.ALLOW_ENHANCEMENT_AS_PROXY, "true" );
		ssrb.applySetting( AvailableSettings.FORMAT_SQL, "false" );
		ssrb.applySetting( AvailableSettings.GENERATE_STATISTICS, "true" );
	}

	@Override
	protected void configureSessionFactoryBuilder(SessionFactoryBuilder sfb) {
		super.configureSessionFactoryBuilder( sfb );
		sfb.applyStatisticsSupport( true );
		sfb.applySecondLevelCacheSupport( false );
		sfb.applyQueryCacheSupport( false );
	}

	@Override
	protected void applyMetadataSources(MetadataSources sources) {
		super.applyMetadataSources( sources );
		sources.addAnnotatedClass( User.class );
		sources.addAnnotatedClass( Office.class );
		sources.addAnnotatedClass( Client.class );
		sources.addAnnotatedClass( Request.class );
		sources.addAnnotatedClass( InternalRequest.class );
		sources.addAnnotatedClass( Phone.class );
	}

	private Long userId;
	private Long targetUserId;
	private Long officeId;

	@Before
	public void setUp() {
		inTransaction(
				session -> {
					Log log = new Log();
					log.setCreationDate( OffsetDateTime.now() );


					Office office = buildOffice( "The office", "And", Collections.emptySet() );

					session.persist( office );
					officeId = office.getId();

					User user = new User();
					user.setOffice( office );
					user.setClient( office.getClient() );
					user.setName( "Fab" );
					user.setLog( log );
					user.setEmail( "fab@hibernate.org" );

					session.persist( user );

					userId = user.getId();

					user = new User();
					user.setOffice( office );
					user.setClient( office.getClient() );
					user.setName( "And" );
					user.setLog( log );
					user.setEmail( "and@hibernate.org" );

					session.persist( user );
					targetUserId = user.getId();
				}
		);
	}

	@After
	public void tearDown() {
		inTransaction(
				session -> {
					session.createQuery( "delete from Request" ).executeUpdate();
					session.createQuery( "delete from User" ).executeUpdate();
					session.createQuery( "delete from Office" ).executeUpdate();
					session.createQuery( "delete from Client" ).executeUpdate();
					session.createQuery( "delete from Phone" ).executeUpdate();
				}
		);
	}

	@Test
	public void testPersist() {
		final Statistics stats = sessionFactory().getStatistics();
		stats.clear();

		InternalRequest internalRequest = new InternalRequest( 1L );
		inTransaction(
				session -> {
					User user = session.find( User.class, userId );
					internalRequest.setUser( user );
					assertThat( stats.getPrepareStatementCount(), is( 1L ) );

					User targetUser = session.find( User.class, targetUserId );
					assertThat( stats.getPrepareStatementCount(), is( 2L ) );

					internalRequest.setTargetUser( targetUser );

					session.persist( internalRequest );

				}
		);
		assertThat( stats.getPrepareStatementCount(), is( 3L ) );
	}

	@Test
	public void testDelete() {
		Set<Phone> officePhones = new HashSet<>();
		officePhones.add( new Phone( 1L, "landline", "028-234-9876" ) );
		officePhones.add( new Phone( 2L, "mobile", "072-122-9876" ) );
		Office office = buildOffice( "second office", "Fab", officePhones );
		inTransaction(
				session -> {
					session.save( office );
				}
		);

		inTransaction(
				session -> {
					Office result = session.find( Office.class, office.id );
					session.delete( result );
				}
		);


		inTransaction(
				session -> {
					List<Office> offices = session.createQuery( "from Office" ).list();
					assertThat( offices.size(), is( 1 ) );
					assertThat( offices.get( 0 ).getId(), is( officeId ) );

					List<Phone> phones = session.createQuery( "from Phone" ).list();
					assertThat( phones.size(), is( 0 ) );
				}
		);
	}

	@Test
	public void testUpdate() {
		InternalRequest internalRequest = new InternalRequest( 1L );
		inTransaction(
				session -> {
					User user = session.find( User.class, userId );
					internalRequest.setUser( user );

					User targetUser = session.find( User.class, targetUserId );

					internalRequest.setTargetUser( targetUser );

					session.persist( internalRequest );
				}
		);

		inTransaction(
				session -> {
					InternalRequest result = session.find( InternalRequest.class, internalRequest.getId() );
					assertThat( result.getTargetUser().getId(), is( targetUserId ) );
					assertThat( result.getUser().getId(), is( userId ) );
					result.setUser( null );
				}
		);

		inTransaction(
				session -> {
					InternalRequest result = session.find( InternalRequest.class, internalRequest.getId() );
					assertThat( result.getTargetUser().getId(), is( targetUserId ) );
					assertThat( result.getUser(), is( nullValue() ) );

					User user = session.find( User.class, userId );
					result.setTargetUser( user );

				}
		);

		inTransaction(
				session -> {
					InternalRequest result = session.find( InternalRequest.class, internalRequest.getId() );
					assertThat( result.getTargetUser().getId(), is( userId ) );
					assertThat( result.getUser(), is( nullValue() ) );

					User user = session.find( User.class, userId );
					result.setUser( user );

					Set<Phone> officePhones = new HashSet<>();
					officePhones.add( new Phone( 1L, "landline", "028-234-9876" ) );
					officePhones.add( new Phone( 2L, "mobile", "072-122-9876" ) );
					Office office = buildOffice( "second office", "Fab", officePhones );


					session.save( office );

					List<Office> offices = new ArrayList<>();
					offices.add( office );

					user.setOffices( offices );
				}
		);

		inTransaction(
				session -> {
					InternalRequest result = session.find( InternalRequest.class, internalRequest.getId() );
					assertThat( result.getTargetUser().getId(), is( userId ) );
					User user = result.getUser();
					assertThat( user.getId(), is( userId ) );
					List<Office> offices = user.getOffices();
					assertThat( offices.size(), is( 1 ) );
					Office office = offices.get( 0 );
					assertThat( office.getPhones().size(), is( 2 ) );
				}
		);
	}

	private Office buildOffice(String officename, String clientName, Set<Phone> phones) {
		Log log = new Log();
		log.setCreationDate( OffsetDateTime.now() );

		Office office;
		office = new Office();
		Client client = new Client();
		client.setName( clientName );
		client.setLog( log );

		office.setName( officename );
		office.setActive( true );
		office.setDescription( officename );
		office.setManaged( true );
		office.setLog( log );
		office.setClient( client );
		office.setPhones( phones );

		return office;
	}

	@Embeddable
	public static class Log {
		@Column(name = "`creationDate`", nullable = false)
		private OffsetDateTime creationDate;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "`idCreator`")
		private User creator;

		public OffsetDateTime getCreationDate() {
			return creationDate;
		}

		public void setCreationDate(OffsetDateTime creationDate) {
			this.creationDate = creationDate;
		}


		public User getCreator() {
			return creator;
		}

		public void setCreator(User creator) {
			this.creator = creator;
		}
	}

	@Entity(name = "User")
	@Table(name = "`User`")
	public static class User {
		@Id
		@GeneratedValue
		private Long id;

		@Column(length = 120, nullable = false)
		private String name;

		@Column(length = 200, nullable = false, unique = true)
		private String email;

		private String hash;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "idOffice")
		private Office office;

		@OneToMany
		private List<Office> offices;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "`idClient`")
		private Client client;

		@Embedded
		private Log log = new Log();

		public Long getId() {
			return id;
		}

		public Office getOffice() {
			return office;
		}

		public void setOffice(Office office) {
			this.office = office;
		}

		public Log getLog() {
			return log;
		}

		public void setLog(Log log) {
			this.log = log;
		}

		public Client getClient() {
			return client;
		}

		public void setClient(Client client) {
			this.client = client;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		public String getHash() {
			return hash;
		}

		public void setHash(String hash) {
			this.hash = hash;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public List<Office> getOffices() {
			return offices;
		}

		public void setOffices(List<Office> offices) {
			this.offices = offices;
		}
	}

	@Entity(name = "Office")
	public static class Office {

		@Id
		@GeneratedValue
		private Long id;

		@Column(length = 50, nullable = false)
		private String name;

		private String description;

		@Column(nullable = false)
		private Boolean isActive = true;

		@Column(nullable = false)
		private Boolean isManaged = false;

		@ManyToOne(optional = false, cascade = CascadeType.ALL)
		@JoinColumn(name = "idClient")
		private Client client;

		@OneToMany(cascade = CascadeType.ALL)
		private Set<Phone> phones = new HashSet<>();

		@Embedded
		private Log log = new Log();

		public Long getId() {
			return id;
		}

		public Client getClient() {
			return client;
		}

		public void setClient(Client client) {
			this.client = client;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public Boolean getActive() {
			return isActive;
		}

		public void setActive(Boolean active) {
			isActive = active;
		}

		public Boolean getManaged() {
			return isManaged;
		}

		public void setManaged(Boolean managed) {
			isManaged = managed;
		}

		public Log getLog() {
			return log;
		}

		public void setLog(Log log) {
			this.log = log;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Set<Phone> getPhones() {
			return phones;
		}

		public void setPhones(Set<Phone> phones) {
			this.phones = phones;
		}
	}

	@Entity(name = "Phone")
	public static class Phone {
		@Id
		private Long id;

		private String type;

		@NaturalId
		@Column(name = "`number`")
		private String number;

		public Phone() {
		}

		public Phone(Long id, String type, String number) {
			this.id = id;
			this.type = type;
			this.number = number;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getNumber() {
			return number;
		}

		public void setNumber(String number) {
			this.number = number;
		}
	}

	@Entity(name = "Client")
	public static class Client {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@Embedded
		private Log log = new Log();

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Log getLog() {
			return log;
		}

		public void setLog(Log log) {
			this.log = log;
		}
	}

	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(discriminatorType = DiscriminatorType.STRING, name = "type", length = 30)
	@Cacheable
	@Entity(name = "Request")
	public static abstract class Request {

		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "idUser")
		private User user;

		@Column(name = "`creationDate`", nullable = false)
		private OffsetDateTime creationDate = OffsetDateTime.now();

		@Enumerated(EnumType.STRING)
		@Column(length = 30, nullable = false, name = "status")
		private StatusRequest status = StatusRequest.REQUESTED;

		Request() {
		}

		public Request(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public User getUser() {
			return user;
		}

		public void setUser(User userSolicitacao) {
			this.user = userSolicitacao;
		}

		public OffsetDateTime getCreationDate() {
			return creationDate;
		}

		public void setCreationDate(OffsetDateTime creationDate) {
			this.creationDate = creationDate;
		}

		public StatusRequest getStatus() {
			return status;
		}

		public void setStatus(StatusRequest status) {
			this.status = status;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "InternalRequest")
	@DiscriminatorValue(value = "INTERN")
	public static class InternalRequest extends Request {

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "`idTargetUser`")
		private User targetUser;

		InternalRequest() {
		}

		public InternalRequest(Long id) {
			super( id );
		}


		public User getTargetUser() {
			return targetUser;
		}

		public void setTargetUser(User targetUser) {
			this.targetUser = targetUser;
		}
	}

	public enum StatusRequest {

		REQUESTED( "requested" ), WAITING( "Feedback waiting" );

		private final String description;

		StatusRequest(final String description) {
			this.description = description;
		}

		public String getDescription() {
			return description;
		}
	}
}
