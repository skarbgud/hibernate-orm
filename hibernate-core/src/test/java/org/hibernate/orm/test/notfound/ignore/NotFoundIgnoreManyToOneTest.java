/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.notfound.ignore;

import java.io.Serializable;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.Hibernate;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.assertj.core.api.Assertions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for `@ManyToOne @NotFound(IGNORE)`
 *
 * NOTES:<ol>
 *     <li>`@NotFound` should force the association to be eager - `Coin#currency` should be loaded immediately</li>
 *     <li>`IGNORE` says to treat the broken fk as null</li>
 * </ol>
 *
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = { NotFoundIgnoreManyToOneTest.Coin.class, NotFoundIgnoreManyToOneTest.Currency.class } )
@SessionFactory( useCollectingStatementInspector = true )
public class NotFoundIgnoreManyToOneTest {

	@Test
	@JiraKey( "HHH-15060" )
	public void testProxy(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			// the non-existent Child
			// 	- this is the one valid deviation from treating the broken fk as null
			final Currency proxy = session.byId( Currency.class ).getReference( 1 );
			try {
				Hibernate.initialize( proxy );
				Assertions.fail( "Expecting ObjectNotFoundException" );
			}
			catch (ObjectNotFoundException expected) {
				assertThat( expected.getEntityName() ).endsWith( "Currency" );
				assertThat( expected.getIdentifier() ).isEqualTo( 1 );
			}
		} );
	}

	@Test
	@JiraKey( "HHH-15060" )
	public void testGet(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			final Coin coin = session.get( Coin.class, 1 );
			assertThat( coin.getCurrency() ).isNull();

			// technically we could use a subsequent-select rather than a join...
			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " join " );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " inner " );
		} );
	}

	@Test
	@JiraKey( "HHH-15060" )
	@FailureExpected( reason = "Bad results due to cross-join" )
	public void testQueryImplicitPathDereferencePredicate(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			final String hql = "select c from Coin c where c.currency.id = 1";
			final List<Coin> coins = session.createSelectionQuery( hql, Coin.class ).getResultList();
			assertThat( coins ).hasSize( 1 );
			assertThat( coins.get( 0 ).getCurrency() ).isNull();

			// technically we could use a subsequent-select rather than a join...
			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " join " );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " inner " );
		} );
	}

	@Test
	@JiraKey( "HHH-15060" )
	public void testQueryOwnerSelection(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			final String hql = "select c from Coin c";
			final List<Coin> coins = session.createSelectionQuery( hql, Coin.class ).getResultList();
			assertThat( coins ).hasSize( 1 );
			assertThat( coins.get( 0 ).getCurrency() ).isNull();

			// at the moment this uses a subsequent-select.  on the bright side, it is at least eagerly fetched.
			assertThat( statementInspector.getSqlQueries() ).hasSize( 2 );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " from Coin " );
			assertThat( statementInspector.getSqlQueries().get( 1 ) ).contains( " from Currency " );

			// but I believe a jon would be better
//			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
//			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " join " );
//			assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " inner " );
		} );
	}

	@Test
	@JiraKey( "HHH-15060" )
	@FailureExpected(
			reason = "Has zero results because of inner-join; & the select w/ inner-join is executed twice for some odd reason"
	)
	public void testQueryAssociationSelection(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			final String hql = "select c.currency from Coin c";
			session.createQuery( hql, Currency.class ).getResultList();
			final List<Currency> currencies = session.createSelectionQuery( hql, Currency.class ).getResultList();
			assertThat( currencies ).hasSize( 1 );
			assertThat( currencies.get( 0 ) ).isNull();

			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( " join " );
			assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( " inner " );
		} );
	}

	@BeforeEach
	protected void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			Currency euro = new Currency( 1, "Euro" );
			Coin fiveC = new Coin( 1, "Five cents", euro );

			session.persist( euro );
			session.persist( fiveC );
		} );

		scope.inTransaction( (session) -> {
			session.createMutationQuery( "delete Currency where id = 1" ).executeUpdate();
		} );
	}

	@AfterEach
	protected void dropTestData(SessionFactoryScope scope) throws Exception {
		scope.inTransaction( (session) -> {
			session.createMutationQuery( "delete Coin where id = 1" ).executeUpdate();
		} );
	}

	@Entity(name = "Coin")
	public static class Coin {
		private Integer id;
		private String name;
		private Currency currency;

		public Coin() {
		}

		public Coin(Integer id, String name, Currency currency) {
			this.id = id;
			this.name = name;
			this.currency = currency;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@ManyToOne(fetch = FetchType.EAGER)
		@NotFound(action = NotFoundAction.IGNORE)
		public Currency getCurrency() {
			return currency;
		}

		public void setCurrency(Currency currency) {
			this.currency = currency;
		}
	}

	@Entity(name = "Currency")
	public static class Currency implements Serializable {
		private Integer id;
		private String name;

		public Currency() {
		}

		public Currency(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
