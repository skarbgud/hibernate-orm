/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.function;

import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;

import java.util.List;

/**
 * Support for SqmFunctionTemplates that ultimately want to
 * perform SQL rendering themselves
 *
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface FunctionRenderingSupport {
	void render(
			SqlAppender sqlAppender,
			List<SqlAstNode> sqlAstArguments,
			SqlAstWalker walker);
}