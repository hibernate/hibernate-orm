/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.functional.cluster;

import java.util.Iterator;
import java.util.List;

import org.hibernate.cache.infinispan.util.InfinispanMessageLogger;
import org.hibernate.test.cache.infinispan.functional.entities.Account;
import org.hibernate.test.cache.infinispan.functional.entities.AccountHolder;

import org.hibernate.Query;
import org.hibernate.SessionFactory;

import static org.hibernate.test.cache.infinispan.util.TxUtil.withTxSession;
import static org.hibernate.test.cache.infinispan.util.TxUtil.withTxSessionApply;

/**
 * @author Brian Stansberry
 */
public class AccountDAO {
	private static final InfinispanMessageLogger log = InfinispanMessageLogger.Provider.getLog(AccountDAO.class);

	private final boolean useJta;
	private final SessionFactory sessionFactory;

	private AccountHolder smith = new AccountHolder("Smith", "1000");
	private AccountHolder jones = new AccountHolder("Jones", "2000");
	private AccountHolder barney = new AccountHolder("Barney", "3000");

	public AccountDAO(boolean useJta, SessionFactory sessionFactory) throws Exception {
		this.useJta = useJta;
		this.sessionFactory = sessionFactory;
	}

	public AccountHolder getSmith() {
		return smith;
	}

	public AccountHolder getJones() {
		return jones;
	}

	public AccountHolder getBarney() {
		return barney;
	}

	public void updateAccountBranch(Integer id, String branch) throws Exception {
		withTxSession(useJta, sessionFactory, session -> {
			log.debug("Updating account " + id + " to branch " + branch);
			Account account = session.get(Account.class, id);
			log.debug("Set branch " + branch);
			account.setBranch(branch);
			session.update(account);
			log.debug("Updated account " + id + " to branch " + branch);
		});
	}

	public int getCountForBranch(String branch, boolean useRegion) throws Exception {
		return withTxSessionApply(useJta, sessionFactory, session -> {
			Query query = session.createQuery(
					"select account from Account as account where account.branch = :branch");
			query.setString("branch", branch);
			if (useRegion) {
				query.setCacheRegion("AccountRegion");
			}
			query.setCacheable(true);
			return query.list().size();
		});
	}

	public void createAccount(AccountHolder holder, Integer id, Integer openingBalance, String branch) throws Exception {
		withTxSession(useJta, sessionFactory, session -> {
			log.debug("Creating account " + id);
			Account account = new Account();
			account.setId(id);
			account.setAccountHolder(holder);
			account.setBalance(openingBalance);
			log.debug("Set branch " + branch);
			account.setBranch(branch);
			session.persist(account);
			log.debug("Created account " + id);
		});
	}

	public Account getAccount(Integer id) throws Exception {
		return withTxSessionApply(useJta, sessionFactory, session -> {
			log.debug("Getting account " + id);
			return session.get(Account.class, id);
		});
	}

	public Account getAccountWithRefresh(Integer id) throws Exception {
		return withTxSessionApply(useJta, sessionFactory, session -> {
			log.debug("Getting account " + id + " with refresh");
			Account acct = session.get(Account.class, id);
			session.refresh(acct);
			return session.get(Account.class, id);
		});
	}

	public void updateAccountBalance(Integer id, Integer newBalance) throws Exception {
		withTxSession(useJta, sessionFactory, session -> {
			log.debug("Updating account " + id + " to balance " + newBalance);
			Account account = session.get(Account.class, id);
			account.setBalance(newBalance);
			session.update(account);
			log.debug("Updated account " + id + " to balance " + newBalance);
		});
	}

	public String getBranch(Object holder, boolean useRegion) throws Exception {
		return withTxSessionApply(useJta, sessionFactory, session -> {
			Query query = session.createQuery(
					"select account.branch from Account as account where account.accountHolder = ?");
			query.setParameter(0, holder);
			if (useRegion) {
				query.setCacheRegion("AccountRegion");
			}
			query.setCacheable(true);
			return (String) query.list().get(0);
		});
	}

	public int getTotalBalance(AccountHolder holder, boolean useRegion) throws Exception {
		List results = (List) withTxSessionApply(useJta, sessionFactory, session -> {
			Query query = session.createQuery(
					"select account.balance from Account as account where account.accountHolder = ?");
			query.setParameter(0, holder);
			if (useRegion) {
				query.setCacheRegion("AccountRegion");
			}
			query.setCacheable(true);
			return query.list();
		});
		int total = 0;
		if (results != null) {
			for (Iterator it = results.iterator(); it.hasNext();) {
				total += ((Integer) it.next()).intValue();
				System.out.println("Total = " + total);
			}
		}
		return total;
	}

	public void cleanup() throws Exception {
		internalCleanup();
	}

	private void internalCleanup() throws Exception {
		withTxSession(useJta, sessionFactory, session -> {
			Query query = session.createQuery("select account from Account as account");
			List accts = query.list();
			if (accts != null) {
				for (Iterator it = accts.iterator(); it.hasNext(); ) {
					try {
						Object acct = it.next();
						log.info("Removing " + acct);
						session.delete(acct);
					} catch (Exception ignored) {
					}
				}
			}
		});
	}

	public void remove() {
		try {
			internalCleanup();
		} catch (Exception e) {
			log.error("Caught exception in remove", e);
		}
	}
}
