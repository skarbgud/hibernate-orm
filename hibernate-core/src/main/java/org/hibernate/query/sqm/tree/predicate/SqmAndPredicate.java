/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.predicate;

import java.util.Arrays;
import java.util.List;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;

import jakarta.persistence.criteria.Expression;

/**
 * @author Steve Ebersole
 */
public class SqmAndPredicate extends AbstractSqmPredicate implements SqmJunctivePredicate {
	private final SqmPredicate leftHandPredicate;
	private final SqmPredicate rightHandPredicate;

	public SqmAndPredicate(
			SqmPredicate leftHandPredicate,
			SqmPredicate rightHandPredicate,
			NodeBuilder nodeBuilder) {
		super( leftHandPredicate.getExpressible(), nodeBuilder );
		this.leftHandPredicate = leftHandPredicate;
		this.rightHandPredicate = rightHandPredicate;
	}

	@Override
	public SqmAndPredicate copy(SqmCopyContext context) {
		final SqmAndPredicate existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmAndPredicate predicate = context.registerCopy(
				this,
				new SqmAndPredicate(
						leftHandPredicate.copy( context ),
						rightHandPredicate.copy( context ),
						nodeBuilder()
				)
		);
		copyTo( predicate, context );
		return predicate;
	}

	@Override
	public SqmPredicate getLeftHandPredicate() {
		return leftHandPredicate;
	}

	@Override
	public SqmPredicate getRightHandPredicate() {
		return rightHandPredicate;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitAndPredicate( this );
	}

	@Override
	public BooleanOperator getOperator() {
		return BooleanOperator.AND;
	}

	@Override
	public boolean isNegated() {
		return false;
	}

	@Override
	public List<Expression<Boolean>> getExpressions() {
		return Arrays.asList( leftHandPredicate, rightHandPredicate );
	}

	@Override
	public SqmPredicate not() {
		return new SqmNegatedPredicate( this, nodeBuilder() );
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		if ( leftHandPredicate instanceof SqmOrPredicate ) {
			sb.append( '(' );
			leftHandPredicate.appendHqlString( sb );
			sb.append( ')' );
		}
		else {
			leftHandPredicate.appendHqlString( sb );
		}
		sb.append( " and " );
		if ( rightHandPredicate instanceof SqmOrPredicate ) {
			sb.append( '(' );
			rightHandPredicate.appendHqlString( sb );
			sb.append( ')' );
		}
		else {
			rightHandPredicate.appendHqlString( sb );
		}
	}
}
