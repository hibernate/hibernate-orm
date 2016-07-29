package org.hibernate.test.onetomany.inheritance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.Map.Entry;

import javax.persistence.InheritanceType;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@TestForIssue(jiraKey = "HHH-11005")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MappedSuperclassMapTest extends BaseNonConfigCoreFunctionalTestCase {
	private static SessionFactory sessionFactory;
	
	private static final String SKU001 = "SKU001";
	private static final String SKU002 = "SKU002";
	private static final String WAR_AND_PEACE = "0140447938";
	private static final String ANNA_KARENINA = "0140449175";

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{
				org.hibernate.test.onetomany.inheritance.joined.BookImpl.class,
				org.hibernate.test.onetomany.inheritance.perclass.BookImpl.class,
				org.hibernate.test.onetomany.inheritance.single.BookImpl.class,
				org.hibernate.test.onetomany.inheritance.joined.LibraryImpl.class,
				org.hibernate.test.onetomany.inheritance.perclass.LibraryImpl.class,
				org.hibernate.test.onetomany.inheritance.single.LibraryImpl.class,
				org.hibernate.test.onetomany.inheritance.joined.ProductImpl.class,
				org.hibernate.test.onetomany.inheritance.perclass.ProductImpl.class,
				org.hibernate.test.onetomany.inheritance.single.ProductImpl.class
		};
	}


/*	
	@AfterClass
	public static void tearDown() {
		if (sessionFactory != null) {
			sessionFactory.close();
		}
	}

	@BeforeClass
	public static void setUp() {
		System.out.println("STARTING TEST ... LOADING SESSION FACTORY ...");
		
		sessionFactory = new Configuration().configure().buildSessionFactory();
		
		System.out.println("SESSION FACTORY LOADED ...");
	}
*/	
	@Test
	public void t01_createEntities_joined() {
		_createEntities(new EntityFactory(InheritanceType.JOINED));
	}
	
	@Test
	public void t02_lookupEntities_joined() {
		_lookupEntities(new EntityFactory(InheritanceType.JOINED));
	}
	
	@Test
	public void t03_lookupEntities_entrySet_joined() {
		_lookupEntities_entrySet(new EntityFactory(InheritanceType.JOINED));
	}
	
	@Test
	public void t04_breakReferences_joined() {
		_breakReferences(new EntityFactory(InheritanceType.JOINED));
	}
	
	@Test
	public void t05_checkNullReferences_joined() {
		_checkNullReferences(new EntityFactory(InheritanceType.JOINED));
	}

	@Test
	public void t11_createEntities_singleTable() {
		_createEntities(new EntityFactory(InheritanceType.SINGLE_TABLE));
	}
	
	@Test
	public void t12_lookupEntities_singleTable() {
		_lookupEntities(new EntityFactory(InheritanceType.SINGLE_TABLE));
	}
	
	@Test
	public void t13_lookupEntities_entrySet_singleTable() {
		_lookupEntities_entrySet(new EntityFactory(InheritanceType.SINGLE_TABLE));
	}
	
	@Test
	public void t14_breakReferences_singleTable() {
		_breakReferences(new EntityFactory(InheritanceType.SINGLE_TABLE));
	}
	
	@Test
	public void t15_checkNullReferences_singleTable() {
		_checkNullReferences(new EntityFactory(InheritanceType.SINGLE_TABLE));
	}

	@Test
	public void t21_createEntities_tablePerClass() {
		_createEntities(new EntityFactory(InheritanceType.TABLE_PER_CLASS));
	}
	
	@Test
	public void t22_lookupEntities_tablePerClass() {
		_lookupEntities(new EntityFactory(InheritanceType.TABLE_PER_CLASS));
	}
	
	@Test
	public void t23_lookupEntities_entrySet_tablePerClass() {
		_lookupEntities_entrySet(new EntityFactory(InheritanceType.TABLE_PER_CLASS));
	}
	
	@Test
	public void t24_breakReferences_tablePerClass() {
		_breakReferences(new EntityFactory(InheritanceType.TABLE_PER_CLASS));
	}
	
	@Test
	public void t25_checkNullReferences_tablePerClass() {
		_checkNullReferences(new EntityFactory(InheritanceType.TABLE_PER_CLASS));
	}

	private void _createEntities(EntityFactory factory) {
		System.out.println("CREATING ENTITIES ...");
		Work work = new Work() {
			public void work(Session sess) {
				Library library = factory.newLibrary();
				Book book = factory.newBook(SKU001, WAR_AND_PEACE, library);
				Book book2 = factory.newBook(SKU002, ANNA_KARENINA, library);
				
				sess.save(library);
				sess.save(book);
				sess.save(book2);

			}
		};
		transact(work);
		System.out.println("... ENTITIES CREATED");
	}
	
	private void _lookupEntities(EntityFactory factory) {
		System.out.println("RETRIEVING ENTITIES ...");
		Work work = new Work() {
			public void work(Session sess) {
				try {
					List libraries = sess.createQuery(factory.libraryPredicate()).list();
					assertEquals(1, libraries.size());
					Library library = (Library) libraries.get(0);
					assertNotNull(library);
					
					assertEquals(2, library.getBooksOnInventory().size());
					
					Book book = library.getBooksOnInventory().get(SKU001);
					assertNotNull(book);
					assertEquals(WAR_AND_PEACE, book.getIsbn());
					
					book = library.getBooksOnInventory().get(SKU002);
					assertNotNull(book);
					assertEquals(ANNA_KARENINA, book.getIsbn());
					
				}
				catch (SQLGrammarException t) {
					t.printStackTrace();
					assertNull(t);
				}
			}
		};
		transact(work);
		System.out.println("... DONE!");
	}
	
	private void _lookupEntities_entrySet(EntityFactory factory) {
		System.out.println("RETRIEVING ENTITIES ...");
		Work work = new Work() {
			public void work(Session sess) {
				try {
					List libraries = sess.createQuery(factory.libraryPredicate()).list();
					assertEquals(1, libraries.size());
					Library library = (Library) libraries.get(0);
					assertNotNull(library);
					
					assertEquals(2, library.getBooksOnInventory().size());
					
					for (Entry<String,Book> entry : library.getBooksOnInventory().entrySet()) {
						System.out.println("Found SKU " + entry.getKey() + " with ISBN " + entry.getValue().getIsbn());
					}
				}
				catch (SQLGrammarException t) {
					t.printStackTrace();
					assertNull(t);
				}
			}
		};
		transact(work);
		System.out.println("... DONE!");
	}
	
	private void _breakReferences(EntityFactory factory) {
		System.out.println("NULLING REFERENCES ...");
		Work work = new Work() {
			public void work(Session sess) {
				List<Book> books = sess.createQuery(factory.bookPredicate()).list();
				assertEquals(2, books.size());
				
				for (Book book : books) {
					assertNotNull(book.getLibrary());
					System.out.println("Found SKU " + book.getInventoryCode() + " with library " + book.getLibrary().getEntid());
				}
				
				try {
					// break the reference between the two
					for (Book book : books) {
						book.setLibrary(null);
					}
				}
				catch (SQLGrammarException t) {
					t.printStackTrace();
					assertNull(t);
				}
			}
		};
		transact(work);
		System.out.println("... DONE!");
	}
	
	private void _checkNullReferences(EntityFactory factory) {
		System.out.println("CHECKING REFERENCES ...");
		Work work = new Work() {
			public void work(Session sess) {
				List<Book> books = sess.createQuery(factory.bookPredicate()).list();
				assertEquals(2, books.size());
				
				for (Book book : books) {
					assertNull(book.getLibrary());
					System.out.println("Found SKU " + book.getInventoryCode() + " with no library");
				}
				
				List libraries = sess.createQuery(factory.libraryPredicate()).list();
				assertEquals(1, libraries.size());
				Library library = (Library) libraries.get(0);
				assertNotNull(library);
				
				assertEquals(0, library.getBooksOnInventory().size());
				System.out.println("Found Library " + library.getEntid() + " with no books");
				
			}
		};
		transact(work);
		System.out.println("... DONE!");
	}

	
	public static void transact(Work work) {
		Session sess = sessionFactory.openSession();
		Transaction tx = sess.beginTransaction();
		work.work(sess);
		tx.commit();
		sess.close();
	}
	
	
	interface Work {
		void work(Session sess);
	}
}
