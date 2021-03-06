/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.javascript.checks;

import com.google.common.collect.Lists;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.javascript.cfg.CfgBlock;
import org.sonar.javascript.cfg.CfgBranchingBlock;
import org.sonar.javascript.cfg.ControlFlowGraph;
import org.sonar.javascript.se.LiveVariableAnalysis;
import org.sonar.javascript.se.LiveVariableAnalysis.Usages;
import org.sonar.javascript.tree.symbols.Scope;
import org.sonar.plugins.javascript.api.symbols.Symbol;
import org.sonar.plugins.javascript.api.symbols.Usage;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.Tree.Kind;
import org.sonar.plugins.javascript.api.tree.declaration.FunctionDeclarationTree;
import org.sonar.plugins.javascript.api.tree.declaration.FunctionTree;
import org.sonar.plugins.javascript.api.tree.declaration.MethodDeclarationTree;
import org.sonar.plugins.javascript.api.tree.expression.ArrowFunctionTree;
import org.sonar.plugins.javascript.api.tree.expression.FunctionExpressionTree;
import org.sonar.plugins.javascript.api.tree.statement.BlockTree;
import org.sonar.plugins.javascript.api.visitors.DoubleDispatchVisitorCheck;

import static org.sonar.javascript.se.LiveVariableAnalysis.isRead;
import static org.sonar.javascript.se.LiveVariableAnalysis.isWrite;

@Rule(key = "S1854")
public class DeadStoreCheck extends DoubleDispatchVisitorCheck {

  private static final String MESSAGE = "Remove this useless assignment to local variable \"%s\"";

  @Override
  public void visitFunctionDeclaration(FunctionDeclarationTree tree) {
    checkFunction(tree);
    super.visitFunctionDeclaration(tree);
  }

  @Override
  public void visitFunctionExpression(FunctionExpressionTree tree) {
    checkFunction(tree);
    super.visitFunctionExpression(tree);
  }

  @Override
  public void visitMethodDeclaration(MethodDeclarationTree tree) {
    checkFunction(tree);
    super.visitMethodDeclaration(tree);
  }

  @Override
  public void visitArrowFunction(ArrowFunctionTree tree) {
    checkFunction(tree);
    super.visitArrowFunction(tree);
  }

  private void checkFunction(FunctionTree functionTree) {
    if (!functionTree.body().is(Kind.BLOCK)) {
      return;
    }

    checkCFG(ControlFlowGraph.build((BlockTree) functionTree.body()), functionTree);
  }

  // Identifying dead stores is basically "live variable analysis".
  // See https://en.wikipedia.org/wiki/Live_variable_analysis
  private void checkCFG(ControlFlowGraph cfg, FunctionTree functionTree) {

    // if there is a try block in the function, we skip the analysis of the whole
    // function, as the CFG and the SE engine currently don't support exception
    // execution paths
    for (CfgBlock cfgBlock : cfg.blocks()) {
      if (isTryBlock(cfgBlock)) {
        return;
      }
    }

    Scope scope = getContext().getSymbolModel().getScope(functionTree);
    LiveVariableAnalysis lva = LiveVariableAnalysis.create(cfg, scope);
    Usages usages = lva.getUsages();

    for (CfgBlock cfgBlock : cfg.blocks()) {
      Set<Symbol> live = lva.getLiveOutSymbols(cfgBlock);
      for (Tree element : Lists.reverse(cfgBlock.elements())) {
        Usage usage = usages.getUsage(element);
        if (usage != null) {
          checkUsage(usage, live, usages);
        }
      }
    }

    raiseIssuesForNeverReadSymbols(usages);
  }
  
  private static boolean isTryBlock(CfgBlock block) {
    if (block instanceof CfgBranchingBlock) {
      CfgBranchingBlock branchingBlock = (CfgBranchingBlock) block;
      return branchingBlock.branchingTree().is(Kind.TRY_STATEMENT);
    }
    return false;
  }

  private void checkUsage(Usage usage, Set<Symbol> liveSymbols, Usages usages) {
    Symbol symbol = usage.symbol();

    if (isWrite(usage)) {
      if (!liveSymbols.contains(symbol) && !usages.hasUsagesInNestedFunctions(symbol) && !usages.neverReadSymbols().contains(symbol)) {
        addIssue(usage.identifierTree(), symbol);
      }
      liveSymbols.remove(symbol);

    } else if (isRead(usage)) {
      liveSymbols.add(symbol);
    }
  }

  private void raiseIssuesForNeverReadSymbols(Usages usages) {
    for (Symbol symbol : usages.neverReadSymbols()) {
      for (Usage usage : symbol.usages()) {
        if (isWrite(usage)) {
          addIssue(usage.identifierTree(), symbol);
        }
      }
    }
  }

  private void addIssue(Tree element, Symbol symbol) {
    addIssue(element, String.format(MESSAGE, symbol.name()));
  }

}
